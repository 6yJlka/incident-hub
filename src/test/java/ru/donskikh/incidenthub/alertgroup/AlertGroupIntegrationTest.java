package ru.donskikh.incidenthub.alertgroup;

import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;
import ru.donskikh.incidenthub.PostgreSQLIntegrationTest;
import ru.donskikh.incidenthub.alert.AlertKey;
import ru.donskikh.incidenthub.alert.AlertRepository;
import ru.donskikh.incidenthub.alert.AlertSignalType;
import ru.donskikh.incidenthub.alert.AlertSeverity;
import ru.donskikh.incidenthub.alert.NewAlert;
import ru.donskikh.incidenthub.alert.correlation.AlertCorrelator;
import ru.donskikh.incidenthub.alert.correlation.CorrelationContext;
import ru.donskikh.incidenthub.alert.correlation.TimeWindowCorrelator;
import ru.donskikh.incidenthub.alertgroup.application.CorrelateAlertsCommand;
import ru.donskikh.incidenthub.alertgroup.application.CorrelateAlertsService;
import ru.donskikh.incidenthub.alertgroup.application.GetAlertGroupQuery;
import ru.donskikh.incidenthub.alertgroup.application.GetAlertGroupService;
import ru.donskikh.incidenthub.alertgroup.application.ListAlertGroupsQuery;
import ru.donskikh.incidenthub.alertgroup.application.ListAlertGroupsService;
import ru.donskikh.incidenthub.catalog.BusinessService;
import ru.donskikh.incidenthub.catalog.BusinessServiceRepository;
import ru.donskikh.incidenthub.catalog.ServiceTier;
import ru.donskikh.incidenthub.team.Team;
import ru.donskikh.incidenthub.team.TeamRepository;
import ru.donskikh.incidenthub.security.JwtTokenService;
import ru.donskikh.incidenthub.identity.UserRole;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.hikari.schema=incident_hub_groups_test",
        "spring.flyway.schemas=incident_hub_groups_test",
        "spring.flyway.default-schema=incident_hub_groups_test",
        "spring.jpa.properties.hibernate.generate_statistics=true"
})
@AutoConfigureMockMvc
class AlertGroupIntegrationTest extends PostgreSQLIntegrationTest {

    private static final Instant BASE = Instant.parse("2026-10-18T12:00:00Z");

    @MockitoSpyBean
    private AlertRepository alerts;

    @MockitoSpyBean
    private AlertCorrelator correlator;

    @Autowired
    private AlertGroupRepository groups;

    @Autowired
    private BusinessServiceRepository services;

    @Autowired
    private TeamRepository teams;

    @Autowired
    private CorrelateAlertsService processor;

    @Autowired
    private ListAlertGroupsService listService;

    @Autowired
    private GetAlertGroupService getService;

    @Autowired
    private TransactionTemplate transactions;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenService tokens;

    private List<BusinessService> fixtureServices;

    @BeforeEach
    void setUp() {
        transactions.executeWithoutResult(status -> {
            jdbc.update("delete from alerts");
            jdbc.update("delete from alert_groups");
            jdbc.update("delete from business_services");
            jdbc.update("delete from teams");
            Team team = teams.save(new Team("Group tests", "GROUP_TESTS"));
            fixtureServices = new ArrayList<>();
            for (int index = 0; index < 3; index++) {
                fixtureServices.add(services.save(new BusinessService("GROUP_SERVICE_" + index,
                        "Group service " + index, "Description", team, ServiceTier.TIER_1)));
            }
        });
    }

    @Test
    void webhookThenTickCreatesSessionsAndRepeatedTickChangesNothing() throws Exception {
        String body = "{\"alerts\":[" + item("first", 0) + "," + item("second", 4) + ","
                + item("third", 8) + "," + item("fourth", 14) + "]}";
        mockMvc.perform(post("/api/v1/alerts").header("X-Integration-Token", "test-integration-token")
                        .contentType("application/json").content(body))
                .andExpect(status().isOk());
        assertThat(groups.count()).isZero();

        var result = processor.process(new CorrelateAlertsCommand(500));

        assertThat(result.processedAlerts()).isEqualTo(4);
        assertThat(result.createdGroups()).isEqualTo(2);
        assertThat(jdbc.queryForObject("select count(*) from alerts where group_id is null", Long.class)).isZero();
        assertThat(groups.findAll()).allSatisfy(group -> assertThat(group.getStatus()).isEqualTo(AlertGroupStatus.OPEN));
        var secondTick = processor.process(new CorrelateAlertsCommand(500));
        assertThat(secondTick.processedAlerts()).isZero();
        assertThat(groups.count()).isEqualTo(2);
        assertThat(jdbc.queryForObject("select count(*) from incidents", Long.class)).isZero();
    }

    @Test
    void limitsBatchAndContinuesSessionAcrossTicksInOccurrenceAndIdOrder() {
        add("late-in-input", 8, 0);
        add("first", 0, 0);
        add("same-time", 0, 1);
        add("middle", 4, 0);
        var first = processor.process(new CorrelateAlertsCommand(2));
        assertThat(first.processedAlerts()).isEqualTo(2);
        var second = processor.process(new CorrelateAlertsCommand(2));
        assertThat(second.processedAlerts()).isEqualTo(2);
        assertThat(groups.count()).isEqualTo(1);
        var group = groups.findAll().getFirst();
        assertThat(group.getFirstAlertAt()).isEqualTo(BASE);
        assertThat(group.getLastAlertAt()).isEqualTo(BASE.plusSeconds(480));
        assertThat(jdbc.queryForObject("select probable_root_service_id from alert_groups", Long.class))
                .isEqualTo(fixtureServices.getFirst().getId());
    }

    @Test
    void loadsOnlyWindowCandidatesAndAllTheirMembersInOneQueryForLateSignals() {
        for (int minute : new int[]{0, 4, 8, 12, 16, 20}) {
            add("series-" + minute, minute, 0);
        }
        processor.process(new CorrelateAlertsCommand(500));
        long chosenId = groups.findAll().getFirst().getId();
        group(-1000, -990, 1, AlertGroupStatus.OPEN);
        group(1000, 1010, 1, AlertGroupStatus.OPEN);
        group(9, 11, 1, AlertGroupStatus.CONFIRMED);
        group(9, 11, 1, AlertGroupStatus.DISMISSED);
        long overlappingId = group(9, 11, 1, AlertGroupStatus.OPEN);
        transactions.executeWithoutResult(status -> {
            var inserted = alerts.insertIfAbsent(List.of(
                    newAlert("overlapping-first", 9, 1), newAlert("overlapping-last", 11, 1)));
            inserted.values().forEach(id -> alerts.attachToGroup(id, overlappingId));
        });
        var contexts = new ArrayList<CorrelationContext>();
        doAnswer(invocation -> {
            CorrelationContext context = invocation.getArgument(1);
            contexts.add(context);
            return new TimeWindowCorrelator(Duration.ofMinutes(5)).correlate(invocation.getArgument(0), context);
        }).when(correlator).correlate(any(), any());
        clearInvocations(alerts);
        add("inside-old-session", 10, 1);

        processor.process(new CorrelateAlertsCommand(500));

        verify(alerts, times(1)).findCorrelationMembers(any());
        assertThat(contexts.getFirst().groups()).hasSize(2);
        assertThat(contexts.getFirst().groups()).filteredOn(group -> group.id() == overlappingId)
                .singleElement().satisfies(group -> assertThat(group.members()).hasSize(2));
        var snapshot = contexts.getFirst().groups().getFirst();
        assertThat(snapshot.id()).isEqualTo(chosenId);
        assertThat(snapshot.members()).hasSize(6);
        assertThat(snapshot.members()).allSatisfy(member -> {
            assertThat(member.businessServiceId()).isEqualTo(fixtureServices.getFirst().getId());
            assertThat(member.signalType()).isEqualTo(AlertSignalType.LATENCY);
            assertThat(member.severity()).isEqualTo(AlertSeverity.WARNING);
        });
        add("earlier", -2, 2);
        processor.process(new CorrelateAlertsCommand(500));
        var result = getService.get(new GetAlertGroupQuery(chosenId));
        assertThat(result.group().probableRootServiceId()).isEqualTo(fixtureServices.get(2).getId());
        assertThat(result.group().firstAlertAt()).isEqualTo(BASE.minusSeconds(120));
        assertThat(result.group().lastAlertAt()).isEqualTo(BASE.plusSeconds(1200));
        assertThat(result.alerts()).hasSize(8);
        assertThat(contexts.getLast().groups().getFirst().members()).hasSize(7);
    }

    @Test
    void skipsConcurrentTickWhileFirstTransactionHoldsAdvisoryLock() throws Exception {
        add("first", 0, 0);
        add("second", 1, 1);
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        doAnswer(invocation -> {
            entered.countDown();
            if (!release.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Timed out waiting for concurrent tick");
            }
            return new TimeWindowCorrelator(Duration.ofMinutes(5))
                    .correlate(invocation.getArgument(0), invocation.getArgument(1));
        }).when(correlator).correlate(any(), any());
        try (var executor = Executors.newSingleThreadExecutor()) {
            var first = executor.submit(() -> processor.process(new CorrelateAlertsCommand(500)));
            try {
                assertThat(entered.await(10, TimeUnit.SECONDS)).isTrue();
                var second = processor.process(new CorrelateAlertsCommand(500));
                assertThat(second.lockAcquired()).isFalse();
                assertThat(second.processedAlerts()).isZero();
            } finally {
                release.countDown();
            }
            assertThat(first.get(15, TimeUnit.SECONDS).processedAlerts()).isEqualTo(2);
        }
        assertThat(groups.count()).isEqualTo(1);
        assertThat(processor.process(new CorrelateAlertsCommand(500)).processedAlerts()).isZero();
    }

    @Test
    void rollsBackEntireBatchAndReleasesLockAfterFailure() {
        add("first", 0, 0);
        add("second", 1, 0);
        AtomicInteger calls = new AtomicInteger();
        doAnswer(invocation -> {
            if (calls.incrementAndGet() == 2) {
                throw new IllegalStateException("Test failure");
            }
            return new TimeWindowCorrelator(Duration.ofMinutes(5))
                    .correlate(invocation.getArgument(0), invocation.getArgument(1));
        }).when(correlator).correlate(any(), any());

        assertThatThrownBy(() -> processor.process(new CorrelateAlertsCommand(500)))
                .isInstanceOf(IllegalStateException.class).hasMessage("Test failure");
        assertThat(groups.count()).isZero();
        assertThat(jdbc.queryForObject("select count(*) from alerts where group_id is null", Long.class)).isEqualTo(2);
        doAnswer(invocation -> new TimeWindowCorrelator(Duration.ofMinutes(5))
                .correlate(invocation.getArgument(0), invocation.getArgument(1)))
                .when(correlator).correlate(any(), any());
        assertThat(processor.process(new CorrelateAlertsCommand(500)).processedAlerts()).isEqualTo(2);
        assertThat(groups.count()).isEqualTo(1);
    }

    @Test
    void listsAndReadsWithoutNPlusOneForDifferentDataSizes() {
        List<Long> ids = new ArrayList<>();
        for (int index = 0; index < 12; index++) {
            ids.add(group(index, index, index % 3, AlertGroupStatus.OPEN));
        }
        var statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        for (int size : new int[]{1, 10}) {
            statistics.clear();
            var page = listService.execute(new ListAlertGroupsQuery(0, size, null, null));
            assertThat(page.items()).hasSize(size);
            assertThat(page.totalElements()).isEqualTo(12);
            assertThat(page.items().getFirst().id()).isEqualTo(ids.getLast());
            assertThat(statistics.getPrepareStatementCount()).isEqualTo(2);
        }
        long groupId = ids.getFirst();
        for (int target : new int[]{1, 10}) {
            for (int index = (target == 1 ? 0 : 1); index < target; index++) {
                final int member = index;
                transactions.executeWithoutResult(status -> {
                    var inserted = alerts.insertIfAbsent(List.of(newAlert("card-" + member, member, member % 3)));
                    alerts.attachToGroup(inserted.values().iterator().next(), groupId);
                });
            }
            statistics.clear();
            var card = getService.get(new GetAlertGroupQuery(groupId));
            assertThat(card.alerts()).hasSize(target);
            assertThat(card.alerts()).allSatisfy(alert -> assertThat(alert.businessServiceCode()).startsWith("GROUP_SERVICE_"));
            assertThat(statistics.getPrepareStatementCount()).isEqualTo(2);
        }
    }

    @Test
    void filtersGroupsAndEnforcesUserAuthenticationForReads() throws Exception {
        long wanted = group(0, 2, 0, AlertGroupStatus.OPEN);
        group(0, 4, 1, AlertGroupStatus.OPEN);
        group(0, 6, 0, AlertGroupStatus.DISMISSED);
        for (UserRole role : UserRole.values()) {
            String jwt = jwt(role);
            mockMvc.perform(get("/api/v1/alert-groups").header("Authorization", "Bearer " + jwt)
                            .param("status", "OPEN").param("rootServiceId", fixtureServices.getFirst().getId().toString()))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1))
                    .andExpect(jsonPath("$.items[0].id").value(wanted));
            mockMvc.perform(get("/api/v1/alert-groups/{id}", wanted).header("Authorization", "Bearer " + jwt))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(wanted));
        }
        for (String path : List.of("/api/v1/alert-groups", "/api/v1/alert-groups/" + wanted)) {
            mockMvc.perform(get(path)).andExpect(status().isUnauthorized());
            mockMvc.perform(get(path).header("X-Integration-Token", "test-integration-token"))
                    .andExpect(status().isUnauthorized());
        }
        String jwt = jwt(UserRole.REPORTER);
        mockMvc.perform(get("/api/v1/alert-groups/999999").header("Authorization", "Bearer " + jwt))
                .andExpect(status().isNotFound());
    }

    private String jwt(UserRole role) {
        var user = org.mockito.Mockito.mock(ru.donskikh.incidenthub.identity.User.class);
        org.mockito.Mockito.when(user.getId()).thenReturn(42L);
        org.mockito.Mockito.when(user.getEmail()).thenReturn("reader@example.com");
        org.mockito.Mockito.when(user.getRole()).thenReturn(role);
        return tokens.issue(user).value();
    }

    private void add(String externalId, long minute, int service) {
        transactions.executeWithoutResult(status -> alerts.insertIfAbsent(List.of(newAlert(externalId, minute, service))));
    }

    private NewAlert newAlert(String externalId, long minute, int service) {
        return new NewAlert(new AlertKey("group-test", externalId), fixtureServices.get(service).getId(),
                AlertSignalType.LATENCY, AlertSeverity.WARNING, BASE.plusSeconds(minute * 60), BASE, "Signal");
    }

    private long group(long first, long last, int service, AlertGroupStatus groupStatus) {
        return transactions.execute(status -> {
            var group = new AlertGroup(services.getReferenceById(fixtureServices.get(service).getId()), BASE.plusSeconds(first * 60));
            group.includeAlert(BASE.plusSeconds(last * 60), group.getProbableRootService());
            groups.saveAndFlush(group);
            jdbc.update("update alert_groups set status = ? where id = ?", groupStatus.name(), group.getId());
            return group.getId();
        });
    }

    private String item(String externalId, long minute) {
        return """
                {"source":"group-test","externalId":"%s","serviceCode":"GROUP_SERVICE_0",
                "signalType":"LATENCY","severity":"WARNING","occurredAt":"%s","description":"Signal"}
                """.formatted(externalId, BASE.plusSeconds(minute * 60));
    }
}
