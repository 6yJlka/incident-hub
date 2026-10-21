package ru.donskikh.incidenthub.experiment;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;
import ru.donskikh.incidenthub.PostgreSQLIntegrationTest;
import ru.donskikh.incidenthub.alert.*;
import ru.donskikh.incidenthub.alert.correlation.*;
import ru.donskikh.incidenthub.alert.infrastructure.CorrelationJsonCodec;
import ru.donskikh.incidenthub.alertgroup.application.*;
import ru.donskikh.incidenthub.catalog.*;
import ru.donskikh.incidenthub.team.*;
import ru.donskikh.incidenthub.experiment.graph.*;
import ru.donskikh.incidenthub.experiment.replay.*;
import ru.donskikh.incidenthub.experiment.scenario.ScenarioAlert;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.function.LongFunction;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {"spring.datasource.hikari.schema=incident_hub_replay_test",
        "spring.flyway.schemas=incident_hub_replay_test", "spring.flyway.default-schema=incident_hub_replay_test"})
@AutoConfigureMockMvc
class ReplayProductEquivalenceIntegrationTest extends PostgreSQLIntegrationTest {
    @Autowired private JdbcTemplate jdbc;
    @Autowired private TransactionTemplate transactions;
    @Autowired private BusinessServiceRepository services;
    @Autowired private TeamRepository teams;
    @Autowired private MockMvc mvc;
    @Autowired private CorrelateAlertsService processor;
    @MockitoBean private AlertCorrelator correlator;
    private List<BusinessService> catalog;
    @BeforeEach
    void prepareCatalog() {
        transactions.executeWithoutResult(status -> {
            jdbc.execute("truncate table alerts, alert_groups, business_services, teams cascade");
            var team = teams.save(new Team("Replay team", "REPLAY"));
            catalog = new ArrayList<>();
            for (int i = 0; i < 3; i++) catalog.add(services.save(new BusinessService("REPLAY_" + i,
                    "Replay service " + i, "Replay fixture", team, ServiceTier.TIER_2)));
        });
    }
    @ParameterizedTest
    @CsvSource({"false,1", "false,2", "false,500", "true,2"})
    void webhookAndTicksMatchPureReplay(boolean naive, int batch) throws Exception {
        AlertCorrelator method = naive ? new NaiveGraphRootCorrelator(Duration.ofMinutes(5), NaiveGraphRootParameters.defaults())
                : new TimeWindowCorrelator(Duration.ofMinutes(5));
        when(correlator.correlate(any(), any())).thenAnswer(call -> {
            CorrelationContext context = call.getArgument(1);
            assertThat(context.graph()).isEqualTo(DependencyGraphSnapshot.empty());
            return method.correlate(call.getArgument(0), context);
        });
        var incoming = new ArrayList<ScenarioAlert>();
        var bodies = new ArrayList<String>();
        var origin = Instant.parse("2026-01-01T00:00:00Z");
        long[] offsets = {0, 0, 300, 600, 901, 1201, 1502};
        for (int i = 0; i < offsets.length; i++) {
            String key = "K" + i;
            var service = catalog.get(i % 3);
            var severity = AlertSeverity.values()[i % AlertSeverity.values().length];
            var time = origin.plusSeconds(offsets[i]);
            incoming.add(new ScenarioAlert(key, service.getId(), time, AlertSignalType.LATENCY, severity));
            bodies.add("""
                    {"source":"replay","externalId":"%s","serviceCode":"%s","signalType":"LATENCY",
                     "severity":"%s","occurredAt":"%s","description":"Replay signal"}
                    """.formatted(key, service.getCode(), severity, time));
        }
        mvc.perform(post("/api/v1/alerts").header("X-Integration-Token", "test-integration-token")
                .contentType("application/json").content("{\"alerts\":[" + String.join(",", bodies) + "]}"))
                .andExpect(status().isOk());
        while (processor.process(new CorrelateAlertsCommand(batch)).processedAlerts() > 0) { }
        var observation = new ScenarioObservation(new ServiceGraph(catalog.stream().map(service ->
                new ServiceNode(service.getId(), service.getCode(), service.getName(), service.getTier())).toList(), List.of()), incoming);
        var replay = new ScenarioReplay().replay(observation, method);
        var alertKeys = new TreeMap<Long, String>();
        jdbc.query("select id, external_id from alerts", rs -> { alertKeys.put(rs.getLong(1), rs.getString(2)); });
        var replayKeys = new TreeMap<Long, String>();
        replay.groups().forEach(group -> group.members().forEach(member -> replayKeys.put(member.alert().id(), member.externalKey())));
        var product = jdbc.queryForList("select id, first_alert_at, last_alert_at, root_assessment::text as assessment from alert_groups order by id");
        assertThat(product).hasSize(replay.groups().size());
        assertThat(replay.groups()).hasSize(3);
        for (int i = 0; i < product.size(); i++) {
            var row = product.get(i);
            var expected = replay.groups().get(i);
            assertThat(((java.sql.Timestamp) row.get("first_alert_at")).toInstant()).isEqualTo(expected.snapshot().firstAlertAt());
            assertThat(((java.sql.Timestamp) row.get("last_alert_at")).toInstant()).isEqualTo(expected.snapshot().lastAlertAt());
            assertThat(normalize(CorrelationJsonCodec.readAssessment((String) row.get("assessment")), alertKeys::get))
                    .isEqualTo(normalize(expected.snapshot().rootAssessment(), replayKeys::get));
            var members = jdbc.queryForList("select external_id, link_strength, link_evidence::text as evidence from alerts where group_id = ? order by occurred_at, id", row.get("id"));
            assertThat(members).hasSize(expected.members().size());
            for (int index = 0; index < members.size(); index++) {
                var actual = members.get(index);
                var member = expected.members().get(index);
                assertThat(actual.get("external_id")).isEqualTo(member.externalKey());
                assertThat(actual.get("link_strength")).isEqualTo(member.linkStrength() == null ? null : member.linkStrength().name());
                assertThat(CorrelationJsonCodec.readEvidence((String) actual.get("evidence")).stream().map(e -> normalize(e, alertKeys::get)).toList())
                        .isEqualTo(member.linkEvidence().stream().map(e -> normalize(e, replayKeys::get)).toList());
            }
        }
    }
    private Object normalize(RootAssessment root, LongFunction<String> keys) {
        return root instanceof RootAssessment.Ranked ranked ? List.of(ranked.candidates(), ranked.evidence().stream().map(e -> normalize(e, keys)).toList()) : root;
    }
    private Object normalize(Evidence evidence, LongFunction<String> keys) {
        return switch (evidence) {
            case Evidence.EarliestAlert earliest -> List.of("EARLIEST_ALERT", keys.apply(earliest.alertId()), earliest.serviceId());
            case Evidence.TemporalProximity temporal -> List.of("TEMPORAL_PROXIMITY", keys.apply(temporal.alertId()), temporal.distanceToGroup());
            default -> evidence;
        };
    }
}
