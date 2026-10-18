package ru.donskikh.incidenthub.alert;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.support.TransactionTemplate;
import ru.donskikh.incidenthub.PostgreSQLIntegrationTest;
import ru.donskikh.incidenthub.catalog.BusinessService;
import ru.donskikh.incidenthub.catalog.BusinessServiceRepository;
import ru.donskikh.incidenthub.catalog.ServiceTier;
import ru.donskikh.incidenthub.team.Team;
import ru.donskikh.incidenthub.team.TeamRepository;
import ru.donskikh.incidenthub.alert.application.AcceptAlertsService;
import ru.donskikh.incidenthub.alert.application.AcceptAlertsCommand;
import ru.donskikh.incidenthub.alert.application.IncomingAlert;
import ru.donskikh.incidenthub.alert.application.AlertAcceptanceStatus;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AlertIngestionIntegrationTest extends PostgreSQLIntegrationTest {
    @LocalServerPort
    private int port;

    @Autowired
    private AlertRepository alertRepository;

    @MockitoSpyBean
    private BusinessServiceRepository businessServiceRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private AcceptAlertsService service;

    @Autowired
    private TransactionTemplate transactions;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private BusinessService businessService;
    private Long teamId;

    @BeforeEach
    void setUp() {
        transactions.executeWithoutResult(status -> {
            String suffix = UUID.randomUUID().toString().substring(0, 8);
            Team team = teamRepository.save(new Team("Alert tests", "ALERT_" + suffix));
            teamId = team.getId();
            businessService = businessServiceRepository.save(new BusinessService(
                    "ALERT_" + suffix, "Alert service", "Alert integration test", team, ServiceTier.TIER_1));
        });
    }

    @AfterEach
    void cleanUp() {
        transactions.executeWithoutResult(status -> {
            jdbcTemplate.update("delete from alerts where business_service_id = ?", businessService.getId());
            businessServiceRepository.deleteById(businessService.getId());
            teamRepository.deleteById(teamId);
        });
    }

    @Test
    void enforcesPairUniquenessAndReadsImmutableAlertThroughJpa() {
        IncomingAlert input = input("prometheus", "one");
        var first = service.accept(new AcceptAlertsCommand(List.of(input)));
        var duplicate = service.accept(new AcceptAlertsCommand(List.of(new IncomingAlert(
                "prometheus", "one", "UNKNOWN", AlertSignalType.ERROR_RATE,
                AlertSeverity.CRITICAL, Instant.now(), "Changed body"))));
        var otherSource = service.accept(new AcceptAlertsCommand(List.of(input("grafana", "one"))));
        long id = first.results().getFirst().alertId();
        assertThat(duplicate.results().getFirst().status()).isEqualTo(AlertAcceptanceStatus.DUPLICATE);
        assertThat(duplicate.results().getFirst().alertId()).isEqualTo(id);
        assertThat(otherSource.results().getFirst().alertId()).isNotEqualTo(id);
        assertThat(jdbcTemplate.queryForObject("select count(*) from alerts where business_service_id = ?",
                Long.class, businessService.getId())).isEqualTo(2);
        transactions.executeWithoutResult(status -> {
            Alert alert = alertRepository.findById(id).orElseThrow();
            assertThat(alert.getDescription()).isEqualTo("Original body");
            assertThat(alert.getSeverity()).isEqualTo(AlertSeverity.WARNING);
            assertThat(alert.getSignalType()).isEqualTo(AlertSignalType.LATENCY);
            assertThat(alert.getSource()).isEqualTo("prometheus");
            assertThat(alert.getBusinessService().getId()).isEqualTo(businessService.getId());
            assertThat(alert.getOccurredAt()).isEqualTo(input.occurredAt());
            assertThat(alert.getReceivedAt()).isAfterOrEqualTo(input.occurredAt());
        });
        assertThatThrownBy(() -> transactions.executeWithoutResult(status ->
                jdbcTemplate.update("""
                        insert into alerts (source, external_id, business_service_id, signal_type, severity,
                                            occurred_at, received_at, description)
                        values ('prometheus', 'one', ?, 'LATENCY', 'INFO', now(), now(), 'duplicate')
                        """, businessService.getId())))
                .isInstanceOf(org.springframework.dao.DuplicateKeyException.class)
                .hasMessageContaining("uk_alerts_source_external_id");
    }

    @Test
    void handlesTwoConcurrentHttpRequestsForSameKeyWithoutRollingBack() throws Exception {
        CyclicBarrier bothReadMissingKey = new CyclicBarrier(2);
        doAnswer(invocation -> {
            bothReadMissingKey.await(10, TimeUnit.SECONDS);
            return List.of(businessService);
        }).when(businessServiceRepository).findAllByCodeIn(any());
        String body = """
                {"alerts":[{"source":"prometheus","externalId":"race","serviceCode":"%s",
                "signalType":"LATENCY","severity":"WARNING","occurredAt":"2026-10-18T12:00:00Z",
                "description":"Concurrent signal"}]}
                """.formatted(businessService.getCode());
        try (HttpClient client = HttpClient.newHttpClient()) {
            HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1/alerts"))
                    .timeout(Duration.ofSeconds(20)).header("Content-Type", "application/json")
                    .header("X-Integration-Token", "test-integration-token")
                    .POST(HttpRequest.BodyPublishers.ofString(body)).build();
            var first = client.sendAsync(request, HttpResponse.BodyHandlers.ofString());
            var second = client.sendAsync(request, HttpResponse.BodyHandlers.ofString());
            var responses = List.of(first.get(25, TimeUnit.SECONDS), second.get(25, TimeUnit.SECONDS));
            assertThat(responses).allSatisfy(response -> assertThat(response.statusCode()).isEqualTo(200));
            assertThat(responses).extracting(response -> JsonPath.<String>read(response.body(), "$.results[0].status"))
                    .containsExactlyInAnyOrder("ACCEPTED", "DUPLICATE");
            assertThat(responses).extracting(response -> JsonPath.<Number>read(response.body(), "$.results[0].alertId").longValue())
                    .containsOnly(JsonPath.<Number>read(responses.getFirst().body(), "$.results[0].alertId").longValue());
        }
        assertThat(jdbcTemplate.queryForObject("select count(*) from alerts where business_service_id = ?",
                Long.class, businessService.getId())).isEqualTo(1);
    }

    @Test
    void returnsEmptyResultsForEmptyRepositoryBatches() {
        assertThat(alertRepository.findIdsByKeys(List.of())).isEmpty();
        assertThat(alertRepository.insertIfAbsent(List.of())).isEmpty();
    }

    private IncomingAlert input(String source, String id) {
        return new IncomingAlert(source, id, businessService.getCode(), AlertSignalType.LATENCY,
                AlertSeverity.WARNING, Instant.parse("2026-10-18T11:59:00Z"), "Original body");
    }
}
