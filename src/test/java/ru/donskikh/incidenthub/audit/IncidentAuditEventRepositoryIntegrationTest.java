package ru.donskikh.incidenthub.audit;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import ru.donskikh.incidenthub.incident.IncidentStatus;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class IncidentAuditEventRepositoryIntegrationTest {

    private static final long REPORTER_ID = 30_001L;
    private static final long INCIDENT_ID = 30_002L;

    @Autowired
    private IncidentAuditEventRepository repository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("delete from incident_audit_events");
        jdbcTemplate.update("delete from incidents");
        jdbcTemplate.update("delete from teams");
        jdbcTemplate.update("delete from users");

        jdbcTemplate.update(
                "insert into users (id, email, display_name) values (?, ?, ?)",
                REPORTER_ID, "audit-reporter@example.com", "Audit Reporter"
        );
        jdbcTemplate.update(
                """
                        insert into incidents (
                            id, title, description, category, source, priority, status, reporter_id
                        ) values (?, 'Audit incident', 'Description', 'INFRASTRUCTURE', 'MANUAL', 'HIGH', 'OPEN', ?)
                        """,
                INCIDENT_ID, REPORTER_ID
        );
    }

    @Test
    void persistsCreatedEventWithNullableFromStatusAndStringEnums() {
        IncidentAuditEvent saved = repository.saveAndFlush(new IncidentAuditEvent(
                INCIDENT_ID,
                IncidentAuditEventType.CREATED,
                null,
                IncidentStatus.OPEN
        ));
        Long eventId = saved.getId();
        entityManager.clear();

        IncidentAuditEvent loaded = repository.findById(eventId).orElseThrow();
        assertThat(loaded.getIncidentId()).isEqualTo(INCIDENT_ID);
        assertThat(loaded.getEventType()).isEqualTo(IncidentAuditEventType.CREATED);
        assertThat(loaded.getFromStatus()).isNull();
        assertThat(loaded.getToStatus()).isEqualTo(IncidentStatus.OPEN);
        assertThat(loaded.getCreatedAt()).isNotNull();
        assertThat(jdbcTemplate.queryForObject(
                "select event_type from incident_audit_events where id = ?",
                String.class,
                eventId
        )).isEqualTo("CREATED");
        assertThat(jdbcTemplate.queryForObject(
                "select to_status from incident_audit_events where id = ?",
                String.class,
                eventId
        )).isEqualTo("OPEN");
    }

    @Test
    void returnsIncidentHistoryInStableChronologicalOrder() {
        Instant timestamp = Instant.parse("2026-08-13T10:00:00Z");
        long firstId = insertAuditEvent("ASSIGNED", "OPEN", "ASSIGNED", timestamp);
        long secondId = insertAuditEvent("STARTED", "ASSIGNED", "IN_PROGRESS", timestamp);
        entityManager.clear();

        List<IncidentAuditEvent> events = repository.findByIncidentIdOrderByCreatedAtAscIdAsc(INCIDENT_ID);

        assertThat(events).extracting(IncidentAuditEvent::getId).containsExactly(firstId, secondId);
    }

    @Test
    void rejectsAuditEventForUnknownIncident() {
        assertThatThrownBy(() -> repository.saveAndFlush(new IncidentAuditEvent(
                999_999L,
                IncidentAuditEventType.CANCELLED,
                IncidentStatus.OPEN,
                IncidentStatus.CANCELLED
        ))).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void hasAppliedMigrationsFromV1ThroughV3() {
        List<String> versions = jdbcTemplate.queryForList(
                "select version from flyway_schema_history where success order by installed_rank",
                String.class
        );

        assertThat(versions).containsSequence("1", "2", "3");
    }

    private long insertAuditEvent(
            String eventType,
            String fromStatus,
            String toStatus,
            Instant createdAt
    ) {
        return jdbcTemplate.queryForObject(
                """
                        insert into incident_audit_events (
                            incident_id, event_type, from_status, to_status, created_at
                        ) values (?, ?, ?, ?, ?)
                        returning id
                        """,
                Long.class,
                INCIDENT_ID,
                eventType,
                fromStatus,
                toStatus,
                Timestamp.from(createdAt)
        );
    }
}
