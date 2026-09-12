package ru.donskikh.incidenthub.audit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import ru.donskikh.incidenthub.PostgreSQLIntegrationTest;
import ru.donskikh.incidenthub.incident.IncidentStatus;
import ru.donskikh.incidenthub.incident.application.StartIncidentProgressCommand;
import ru.donskikh.incidenthub.incident.application.StartIncidentProgressService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class IncidentAuditTransactionIntegrationTest extends PostgreSQLIntegrationTest {

    private static final long REPORTER_ID = 40_001L;
    private static final long ASSIGNEE_ID = 40_002L;
    private static final long TEAM_ID = 40_003L;
    private static final long SERVICE_ID = 40_004L;
    private static final long INCIDENT_ID = 40_005L;
    private static final String FAILURE_CONSTRAINT = "chk_test_reject_started_audit";

    @Autowired
    private StartIncidentProgressService service;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("alter table incident_audit_events drop constraint if exists " + FAILURE_CONSTRAINT);
        deleteTestData();
        jdbcTemplate.update(
                "insert into users (id, email, display_name) values (?, ?, ?)",
                REPORTER_ID, "transaction-reporter@example.com", "Transaction Reporter"
        );
        jdbcTemplate.update(
                "insert into users (id, email, display_name) values (?, ?, ?)",
                ASSIGNEE_ID, "transaction-assignee@example.com", "Transaction Assignee"
        );
        jdbcTemplate.update(
                "insert into teams (id, name, code) values (?, ?, ?)",
                TEAM_ID, "Transaction Team", "TRANSACTION_TEAM"
        );
        jdbcTemplate.update(
                """
                        insert into business_services (
                            id, code, name, description, owner_team_id, tier
                        ) values (?, 'TRANSACTION_SERVICE', 'Transaction Service', 'Description', ?, 'TIER_2')
                        """,
                SERVICE_ID, TEAM_ID
        );
        jdbcTemplate.update(
                """
                        insert into incidents (
                            id, title, description, affected_service_id, source, priority, severity,
                            status, reporter_id, assignee_id
                        ) values (?, 'Transactional audit', 'Description', ?, 'MANUAL', 'HIGH', 'SEV2',
                            'ASSIGNED', ?, ?)
                        """,
                INCIDENT_ID, SERVICE_ID, REPORTER_ID, ASSIGNEE_ID
        );
        jdbcTemplate.execute(
                "alter table incident_audit_events add constraint " + FAILURE_CONSTRAINT
                        + " check (event_type <> 'STARTED')"
        );
    }

    @AfterEach
    void tearDown() {
        jdbcTemplate.execute("alter table incident_audit_events drop constraint if exists " + FAILURE_CONSTRAINT);
        deleteTestData();
    }

    @Test
    void rollsBackLifecycleChangeWhenAuditInsertFails() {
        assertThatThrownBy(() -> service.start(new StartIncidentProgressCommand(INCIDENT_ID)))
                .isInstanceOf(DataIntegrityViolationException.class);

        assertThat(jdbcTemplate.queryForObject(
                "select status from incidents where id = ?",
                String.class,
                INCIDENT_ID
        )).isEqualTo(IncidentStatus.ASSIGNED.name());
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from incident_audit_events where incident_id = ?",
                Long.class,
                INCIDENT_ID
        )).isZero();
    }

    private void deleteTestData() {
        jdbcTemplate.update("delete from incident_audit_events where incident_id = ?", INCIDENT_ID);
        jdbcTemplate.update("delete from incidents where id = ?", INCIDENT_ID);
        jdbcTemplate.update("delete from business_services where id = ?", SERVICE_ID);
        jdbcTemplate.update("delete from teams where id = ?", TEAM_ID);
        jdbcTemplate.update("delete from users where id in (?, ?)", REPORTER_ID, ASSIGNEE_ID);
    }
}
