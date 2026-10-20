package ru.donskikh.incidenthub.alertgroup;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.slf4j.LoggerFactory;
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
import ru.donskikh.incidenthub.alert.AlertSeverity;
import ru.donskikh.incidenthub.alert.AlertSignalType;
import ru.donskikh.incidenthub.alert.NewAlert;
import ru.donskikh.incidenthub.alert.correlation.AlertCorrelator;
import ru.donskikh.incidenthub.alert.correlation.RootAssessment;
import ru.donskikh.incidenthub.alert.correlation.TimeWindowCorrelator;
import ru.donskikh.incidenthub.alertgroup.application.ConfirmAlertGroupCommand;
import ru.donskikh.incidenthub.alertgroup.application.ConfirmAlertGroupService;
import ru.donskikh.incidenthub.alertgroup.application.CorrelateAlertsCommand;
import ru.donskikh.incidenthub.alertgroup.application.CorrelateAlertsService;
import ru.donskikh.incidenthub.alertgroup.infrastructure.AlertCorrelationProperties;
import ru.donskikh.incidenthub.alertgroup.infrastructure.AlertCorrelationScheduler;
import ru.donskikh.incidenthub.catalog.BusinessService;
import ru.donskikh.incidenthub.catalog.BusinessServiceRepository;
import ru.donskikh.incidenthub.catalog.ServiceTier;
import ru.donskikh.incidenthub.identity.User;
import ru.donskikh.incidenthub.identity.UserRepository;
import ru.donskikh.incidenthub.identity.UserRole;
import ru.donskikh.incidenthub.incident.IncidentPriority;
import ru.donskikh.incidenthub.incident.IncidentSeverity;
import ru.donskikh.incidenthub.incident.application.CreateIncidentService;
import ru.donskikh.incidenthub.security.JwtTokenService;
import ru.donskikh.incidenthub.team.Team;
import ru.donskikh.incidenthub.team.TeamRepository;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.hikari.schema=incident_hub_group_actions_test",
        "spring.flyway.schemas=incident_hub_group_actions_test",
        "spring.flyway.default-schema=incident_hub_group_actions_test"
})
@AutoConfigureMockMvc
class AlertGroupActionsIntegrationTest extends PostgreSQLIntegrationTest {
    private static final Instant BASE = Instant.parse("2026-10-18T12:00:00Z");
    private static final String CONFIRM = "{\"expectedVersion\":0,\"severity\":\"SEV2\",\"priority\":\"HIGH\"}";

    @Autowired private AlertRepository alerts;
    @Autowired private AlertGroupRepository groups;
    @Autowired private BusinessServiceRepository services;
    @Autowired private TeamRepository teams;
    @Autowired private UserRepository users;
    @Autowired private ConfirmAlertGroupService confirm;
    @Autowired private CorrelateAlertsService processor;
    @Autowired private AlertCorrelationProperties properties;
    @Autowired private TransactionTemplate transactions;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private MockMvc mvc;
    @Autowired private JwtTokenService tokens;
    @MockitoSpyBean private CreateIncidentService createIncident;
    @MockitoSpyBean private AlertCorrelator correlator;

    private User actor;
    private BusinessService root;
    private BusinessService override;

    @BeforeEach
    void setUp() {
        transactions.executeWithoutResult(status -> {
            jdbc.update("delete from alerts");
            jdbc.update("delete from alert_groups");
            jdbc.update("delete from incident_audit_events");
            jdbc.update("delete from incidents");
            jdbc.update("delete from users");
            jdbc.update("delete from business_services");
            jdbc.update("delete from teams");
            actor = users.save(new User("engineer@example.com", "Engineer", "hash", UserRole.ENGINEER));
            var team = teams.save(new Team("Root team", "ROOT_TEAM"));
            var otherTeam = teams.save(new Team("Other team", "OTHER_TEAM"));
            root = services.save(new BusinessService("ROOT", "Root service", "Description", team, ServiceTier.TIER_1));
            override = services.save(new BusinessService("OTHER", "Other service", "Description", otherTeam, ServiceTier.TIER_2));
        });
    }

    @Test
    void confirmsWebhookGroupWithAutomaticIncidentAndAuditAndBacklink() throws Exception {
        mvc.perform(post("/api/v1/alerts").header("X-Integration-Token", "test-integration-token")
                .contentType("application/json").content("""
                {"alerts":[{"source":"test","externalId":"a","serviceCode":"ROOT","signalType":"LATENCY",
                "severity":"INFO","occurredAt":"2026-10-18T12:00:00Z","description":"Signal"}]}
                """)).andExpect(status().isOk());
        processor.process(new CorrelateAlertsCommand(500));
        long id = groups.findAll().getFirst().getId();
        mvc.perform(post("/api/v1/alert-groups/{id}/confirm", id).header("Authorization", bearer())
                        .contentType("application/json").content(confirmBody(id)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.decidedById").value(actor.getId()))
                .andExpect(jsonPath("$.decidedAt").isNotEmpty())
                .andExpect(jsonPath("$.availableActions").isEmpty());
        long incidentId = jdbc.queryForObject("select incident_id from alert_groups where id = ?", Long.class, id);
        mvc.perform(get("/api/v1/incidents/{id}", incidentId).header("Authorization", bearer()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.source").value("AUTOMATIC"))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.severity").value("SEV2"))
                .andExpect(jsonPath("$.priority").value("HIGH"))
                .andExpect(jsonPath("$.reporterId").value(actor.getId()))
                .andExpect(jsonPath("$.responsibleTeamId").value(root.getOwnerTeam().getId()))
                .andExpect(jsonPath("$.alertGroupId").value(id))
                .andExpect(jsonPath("$.title").value("Monitoring alerts: Root service (1)"));
        assertThat(jdbc.queryForObject("select actor_id from incident_audit_events where incident_id = ? and event_type = 'CREATED'",
                Long.class, incidentId)).isEqualTo(actor.getId());
        mvc.perform(post("/api/v1/alert-groups/{id}/confirm", id).header("Authorization", bearer())
                        .contentType("application/json").content(confirmBody(id)))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.type").value("urn:incident-hub:problem:conflict"));
        assertThat(jdbc.queryForObject("select count(*) from incidents", Long.class)).isEqualTo(1);
    }

    @Test
    void overrideUsesSelectedServiceTeam() throws Exception {
        long id = group();
        mvc.perform(post("/api/v1/alert-groups/{id}/confirm", id).header("Authorization", bearer())
                        .contentType("application/json").content("{\"expectedVersion\":0,\"severity\":\"SEV1\",\"priority\":\"LOW\",\"businessServiceId\":"
                                + override.getId() + "}"))
                .andExpect(status().isOk());
        assertThat(jdbc.queryForObject("select affected_service_id from incidents", Long.class)).isEqualTo(override.getId());
        assertThat(jdbc.queryForObject("select responsible_team_id from incidents", Long.class)).isEqualTo(override.getOwnerTeam().getId());
        assertThat(jdbc.queryForObject("select probable_root_service_id from alert_groups", Long.class)).isEqualTo(root.getId());
    }

    @Test
    void dismissesWithoutIncidentAndReturnsStateConflictOnRepeat() throws Exception {
        long id = group();
        mvc.perform(post("/api/v1/alert-groups/{id}/dismiss", id).header("Authorization", bearer())
                        .contentType("application/json").content("{\"expectedVersion\":0,\"reason\":\"Maintenance\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("DISMISSED"))
                .andExpect(jsonPath("$.dismissReason").value("Maintenance"))
                .andExpect(jsonPath("$.decidedById").value(actor.getId()))
                .andExpect(jsonPath("$.availableActions").isEmpty());
        assertThat(jdbc.queryForObject("select count(*) from incidents", Long.class)).isZero();
        mvc.perform(post("/api/v1/alert-groups/{id}/dismiss", id).header("Authorization", bearer())
                        .contentType("application/json").content("{\"expectedVersion\":" + version(id) + "}"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.type").value("urn:incident-hub:problem:conflict"));
    }

    @ParameterizedTest
    @EnumSource(UserRole.class)
    void enforcesRolesAndAvailableActions(UserRole role) throws Exception {
        long id = group();
        var user = new User("role@example.com", "Role", "hash", role);
        org.springframework.test.util.ReflectionTestUtils.setField(user, "id", actor.getId());
        String token = "Bearer " + tokens.issue(user).value();
        mvc.perform(get("/api/v1/alert-groups/{id}", id).header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableActions.length()").value(role == UserRole.REPORTER ? 0 : 2));
        for (String action : List.of("confirm", "dismiss")) {
            mvc.perform(post("/api/v1/alert-groups/{id}/" + action, id)
                            .header("X-Integration-Token", "test-integration-token")
                            .contentType("application/json").content(action.equals("confirm") ? CONFIRM : "{\"expectedVersion\":0}"))
                    .andExpect(status().isUnauthorized());
        }
        if (role == UserRole.REPORTER) {
            for (String action : List.of("confirm", "dismiss")) {
                mvc.perform(post("/api/v1/alert-groups/{id}/" + action, id).header("Authorization", token)
                                .contentType("application/json").content(action.equals("confirm") ? CONFIRM : "{\"expectedVersion\":0}"))
                        .andExpect(status().isForbidden())
                        .andExpect(jsonPath("$.type").value("urn:incident-hub:problem:access-denied"));
            }
        } else {
            mvc.perform(post("/api/v1/alert-groups/{id}/dismiss", id).header("Authorization", token)
                        .contentType("application/json").content("{\"expectedVersion\":0}"))
                    .andExpect(status().isOk());
        }
    }

    @Test
    void simultaneousConfirmationsCommitOneIncidentAndOneAuditEvent() throws Exception {
        long id = group();
        CountDownLatch loaded = new CountDownLatch(2);
        CountDownLatch release = new CountDownLatch(1);
        doAnswer(invocation -> {
            loaded.countDown();
            await(release);
            return invocation.callRealMethod();
        }).when(createIncident).create(any());
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> confirmRequest(id));
            var second = executor.submit(() -> confirmRequest(id));
            try {
                assertThat(loaded.await(10, TimeUnit.SECONDS)).isTrue();
            } finally {
                release.countDown();
            }
            var results = List.of(first.get(15, TimeUnit.SECONDS), second.get(15, TimeUnit.SECONDS));
            assertThat(results).extracting(result -> result.getResponse().getStatus()).containsExactlyInAnyOrder(200, 409);
            var conflict = results.stream().filter(result -> result.getResponse().getStatus() == 409).findFirst().orElseThrow();
            assertThat(conflict.getResponse().getContentAsString()).contains("urn:incident-hub:problem:concurrent-modification");
        }
        assertThat(jdbc.queryForObject("select count(*) from incidents", Long.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select count(*) from incident_audit_events", Long.class)).isEqualTo(1);
        assertThat(groups.findById(id).orElseThrow().getStatus()).isEqualTo(AlertGroupStatus.CONFIRMED);
    }

    @Test
    void databaseEnforcesIncidentOwnershipAndDecisionConsistency() {
        long openId = group();
        long confirmedId = group();
        confirm.confirm(new ConfirmAlertGroupCommand(confirmedId, IncidentSeverity.SEV2, IncidentPriority.HIGH,
                null, null, null, actor.getId(), version(confirmedId)));
        long incidentId = jdbc.queryForObject("select incident_id from alert_groups where id = ?", Long.class, confirmedId);
        assertThatThrownBy(() -> transactions.executeWithoutResult(status -> jdbc.update(
                "update alert_groups set status = 'CONFIRMED', decided_by_id = ?, decided_at = now() where id = ?",
                actor.getId(), openId))).hasMessageContaining("chk_alert_groups_incident_status");
        for (String groupStatus : List.of("OPEN", "DISMISSED")) {
            assertThatThrownBy(() -> transactions.executeWithoutResult(status -> jdbc.update(
                    """
                    update alert_groups set status = ?,
                        decided_by_id = case when ? = 'OPEN' then null else decided_by_id end,
                        decided_at = case when ? = 'OPEN' then null else decided_at end
                    where id = ?
                    """, groupStatus, groupStatus, groupStatus, confirmedId)))
                    .hasMessageContaining("chk_alert_groups_incident_status");
        }
        assertThatThrownBy(() -> transactions.executeWithoutResult(status -> jdbc.update(
                "update alert_groups set status = 'DISMISSED' where id = ?", openId)))
                .hasMessageContaining("chk_alert_groups_decision");
        assertThatThrownBy(() -> transactions.executeWithoutResult(status -> jdbc.update(
                """
                update alert_groups set status = 'CONFIRMED', incident_id = ?,
                    decided_by_id = ?, decided_at = now() where id = ?
                """, incidentId, actor.getId(), openId))).hasMessageContaining("uk_alert_groups_incident");
        assertThat(groups.findById(openId).orElseThrow().getStatus()).isEqualTo(AlertGroupStatus.OPEN);
        assertThat(groups.findIdByIncidentId(incidentId)).contains(confirmedId);
    }

    @Test
    void confirmationDuringUnchangedIntervalRollsBackTickWarnsOnceAndNextTickSucceeds() throws Exception {
        add("first", 0);
        add("last", 4);
        processor.process(new CorrelateAlertsCommand(500));
        long id = groups.findAll().getFirst().getId();
        add("inside", 2);
        CountDownLatch loaded = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        AtomicBoolean pause = new AtomicBoolean(true);
        doAnswer(invocation -> {
            if (pause.getAndSet(false)) {
                loaded.countDown();
                await(release);
            }
            return new TimeWindowCorrelator(Duration.ofMinutes(5))
                    .correlate(invocation.getArgument(0), invocation.getArgument(1));
        }).when(correlator).correlate(any(), any());
        var scheduler = new AlertCorrelationScheduler(processor, properties);
        Logger logger = (Logger) LoggerFactory.getLogger(AlertCorrelationScheduler.class);
        var appender = new ListAppender<ILoggingEvent>();
        appender.start();
        logger.addAppender(appender);
        try (var executor = Executors.newSingleThreadExecutor()) {
            var tick = executor.submit(scheduler::tick);
            try {
                assertThat(loaded.await(10, TimeUnit.SECONDS)).isTrue();
                confirm.confirm(new ConfirmAlertGroupCommand(id, IncidentSeverity.SEV2, IncidentPriority.HIGH,
                        null, null, null, actor.getId(), version(id)));
            } finally {
                release.countDown();
            }
            tick.get(15, TimeUnit.SECONDS);
            assertThat(appender.list).singleElement().satisfies(event -> {
                assertThat(event.getLevel()).isEqualTo(Level.WARN);
                assertThat(event.getFormattedMessage()).contains("Alert group " + id + " changed concurrently");
                assertThat(event.getThrowableProxy()).isNull();
            });
            assertThat(alerts.countByGroup_Id(id)).isEqualTo(2);
            assertThat(jdbc.queryForObject("select count(*) from alerts where group_id is null", Long.class)).isEqualTo(1);
            scheduler.tick();
            assertThat(groups.count()).isEqualTo(2);
            assertThat(jdbc.queryForObject("select count(*) from alerts where group_id is null", Long.class)).isZero();
            assertThat(alerts.countByGroup_Id(id)).isEqualTo(2);
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }
    }

    private org.springframework.test.web.servlet.MvcResult confirmRequest(long id) throws Exception {
        return mvc.perform(post("/api/v1/alert-groups/{id}/confirm", id).header("Authorization", bearer())
                .contentType("application/json").content(confirmBody(id))).andReturn();
    }

    private long version(long id) { return groups.findById(id).orElseThrow().getVersion(); }

    private String confirmBody(long id) {
        return CONFIRM.replace("\"expectedVersion\":0", "\"expectedVersion\":" + version(id));
    }

    private String bearer() { return "Bearer " + tokens.issue(actor).value(); }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"confirm", "dismiss"})
    void rejectsViewedVersionAfterTickAndAcceptsRefreshedVersion(String action) throws Exception {
        add("initial", 0);
        processor.process(new CorrelateAlertsCommand(500));
        long id = groups.findAll().getFirst().getId();
        long viewed = version(id);
        mvc.perform(get("/api/v1/alert-groups/{id}", id).header("Authorization", bearer()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.version").value(viewed));
        add("late", 1);
        processor.process(new CorrelateAlertsCommand(500));
        assertThat(version(id)).isGreaterThan(viewed);
        decision(id, action, viewed).andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("urn:incident-hub:problem:concurrent-modification"));
        assertThat(jdbc.queryForObject("select count(*) from incidents", Long.class)).isZero();
        decision(id, action, version(id)).andExpect(status().isOk());
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"confirm", "dismiss"})
    void staleVersionAfterAnotherDecisionIsConcurrentModification(String action) throws Exception {
        long id = group();
        long viewed = version(id);
        decision(id, "dismiss", viewed).andExpect(status().isOk());
        decision(id, action, viewed).andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("urn:incident-hub:problem:concurrent-modification"));
        decision(id, action, version(id)).andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("urn:incident-hub:problem:conflict"));
    }

    private org.springframework.test.web.servlet.ResultActions decision(long id, String action, long expectedVersion) throws Exception {
        return mvc.perform(post("/api/v1/alert-groups/{id}/" + action, id).header("Authorization", bearer())
                .contentType("application/json").content("{\"expectedVersion\":" + expectedVersion
                        + ",\"severity\":\"SEV2\",\"priority\":\"HIGH\"}"));
    }


    @Test
    void requiresExplicitServiceForUndeterminedAndReturnsDedicatedProblem() throws Exception {
        long id = transactions.execute(status -> groups.saveAndFlush(new AlertGroup(null, BASE,
                new RootAssessment.Undetermined("INSUFFICIENT_EVIDENCE"))).getId());
        mvc.perform(post("/api/v1/alert-groups/{id}/confirm", id).header("Authorization", bearer())
                .contentType("application/json").content(CONFIRM))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("urn:incident-hub:problem:root-service-required"))
                .andExpect(jsonPath("$.detail").value("businessServiceId is required when the probable root is undetermined"));
        assertThat(jdbc.queryForObject("select count(*) from incidents", Long.class)).isZero();
        mvc.perform(post("/api/v1/alert-groups/{id}/confirm", id).header("Authorization", bearer())
                .contentType("application/json").content(CONFIRM.replace("}", ",\"businessServiceId\":" + override.getId() + "}")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.rootAssessment.status").value("UNDETERMINED"));
        assertThat(jdbc.queryForObject("select affected_service_id from incidents", Long.class)).isEqualTo(override.getId());
    }

    private long group() {
        return transactions.execute(status -> groups.saveAndFlush(ru.donskikh.incidenthub.CorrelationTestData.group(
                services.getReferenceById(root.getId()), BASE)).getId());
    }

    private void add(String externalId, int minute) {
        transactions.executeWithoutResult(status -> alerts.insertIfAbsent(List.of(new NewAlert(new AlertKey("test", externalId),
                root.getId(), AlertSignalType.LATENCY, AlertSeverity.WARNING, BASE.plusSeconds(minute * 60L), BASE, "Signal"))));
    }

    private static void await(CountDownLatch latch) throws InterruptedException {
        if (!latch.await(10, TimeUnit.SECONDS)) {
            throw new IllegalStateException("Timed out waiting for concurrent transaction");
        }
    }
}
