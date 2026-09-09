package ru.donskikh.incidenthub.incident.infrastructure;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import ru.donskikh.incidenthub.incident.Incident;
import ru.donskikh.incidenthub.incident.IncidentPriority;
import ru.donskikh.incidenthub.incident.IncidentRepository;
import ru.donskikh.incidenthub.incident.IncidentSeverity;
import ru.donskikh.incidenthub.incident.IncidentSource;
import ru.donskikh.incidenthub.incident.IncidentStatus;

import java.sql.Timestamp;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@Transactional
class IncidentRepositoryIntegrationTest {

    private static final long REPORTER_ID = 10_001L;
    private static final long ASSIGNEE_ID = 10_002L;
    private static final long TEAM_ID = 10_003L;
    private static final long INFRASTRUCTURE_SERVICE_ID = 10_004L;
    private static final long APPLICATION_SERVICE_ID = 10_005L;
    private static final long BUSINESS_PROCESS_SERVICE_ID = 10_006L;

    @Autowired
    private IncidentRepository incidentRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("delete from incident_audit_events");
        jdbcTemplate.update("delete from incidents");
        jdbcTemplate.update("delete from service_dependencies");
        jdbcTemplate.update("delete from business_services");
        jdbcTemplate.update("delete from teams");
        jdbcTemplate.update("delete from users");

        insertUser(REPORTER_ID, "reporter-list@example.com", "List Reporter");
        insertUser(ASSIGNEE_ID, "assignee-list@example.com", "List Assignee");
        insertTeam(TEAM_ID, "Platform", "PLATFORM");
        insertBusinessService(INFRASTRUCTURE_SERVICE_ID, "INFRASTRUCTURE", "Infrastructure");
        insertBusinessService(APPLICATION_SERVICE_ID, "APPLICATION", "Application");
        insertBusinessService(BUSINESS_PROCESS_SERVICE_ID, "BUSINESS_PROCESS", "Business Process");

        insertIncident(20_001L, "Older open", INFRASTRUCTURE_SERVICE_ID, "MANUAL", "HIGH", "SEV2",
                "OPEN", null, null, "2026-08-01T12:00:00Z");
        insertIncident(20_002L, "Newer id at same time", INFRASTRUCTURE_SERVICE_ID, "AUTOMATIC", "HIGH",
                "SEV2", "OPEN", ASSIGNEE_ID, TEAM_ID, "2026-08-01T12:00:00Z");
        insertIncident(20_003L, "Application closed", APPLICATION_SERVICE_ID, "MANUAL", "LOW", "SEV4",
                "CLOSED", null, null, "2026-08-01T13:00:00Z");
        insertIncident(20_004L, "Business process open", BUSINESS_PROCESS_SERVICE_ID, "MANUAL", "LOW",
                "SEV3", "OPEN", null, null, "2026-08-01T11:00:00Z");
        insertIncident(20_005L, "Newest critical", INFRASTRUCTURE_SERVICE_ID, "AUTOMATIC", "CRITICAL",
                "SEV1", "IN_PROGRESS", ASSIGNEE_ID, TEAM_ID, "2026-08-01T14:00:00Z");
        entityManager.clear();
    }

    @Test
    void findsAllWithStableCreatedAtAndIdDescendingSort() {
        Page<Incident> result = findAll(null, null, null, null, null, null, 0, 10);

        assertThat(result.getContent()).extracting(Incident::getId)
                .containsExactly(20_005L, 20_003L, 20_002L, 20_001L, 20_004L);
    }

    @Test
    void filtersByEachOptionalField() {
        Page<Incident> byStatus = findAll(IncidentStatus.OPEN, null, null, null, null, null, 0, 10);
        Page<Incident> byPriority = findAll(null, IncidentPriority.LOW, null, null, null, null, 0, 10);
        Page<Incident> bySeverity = findAll(null, null, IncidentSeverity.SEV1, null, null, null, 0, 10);
        Page<Incident> bySource = findAll(null, null, null, IncidentSource.AUTOMATIC, null, null, 0, 10);
        Page<Incident> byService = findAll(
                null, null, null, null, INFRASTRUCTURE_SERVICE_ID, null, 0, 10
        );
        Page<Incident> byTeam = findAll(null, null, null, null, null, TEAM_ID, 0, 10);

        assertThat(byStatus.getContent()).extracting(Incident::getId)
                .containsExactly(20_002L, 20_001L, 20_004L);
        assertThat(byPriority.getContent()).extracting(Incident::getId)
                .containsExactly(20_003L, 20_004L);
        assertThat(bySeverity.getContent()).extracting(Incident::getId)
                .containsExactly(20_005L);
        assertThat(bySource.getContent()).extracting(Incident::getId)
                .containsExactly(20_005L, 20_002L);
        assertThat(byService.getContent()).extracting(Incident::getId)
                .containsExactly(20_005L, 20_002L, 20_001L);
        assertThat(byTeam.getContent()).extracting(Incident::getId)
                .containsExactly(20_005L, 20_002L);
    }

    @Test
    void combinesFilters() {
        Page<Incident> result = findAll(
                IncidentStatus.OPEN,
                IncidentPriority.HIGH,
                IncidentSeverity.SEV2,
                IncidentSource.AUTOMATIC,
                INFRASTRUCTURE_SERVICE_ID,
                TEAM_ID,
                0,
                10
        );

        assertThat(result.getContent()).extracting(Incident::getId)
                .containsExactly(20_002L);
    }

    @Test
    void returnsEmptyPageForUnknownAffectedService() {
        Page<Incident> result = findAll(null, null, null, null, 999_999L, null, 0, 10);

        assertThat(result).isEmpty();
    }

    @Test
    void returnsEmptyPageForUnknownResponsibleTeam() {
        Page<Incident> result = findAll(null, null, null, null, null, 999_999L, 0, 10);

        assertThat(result).isEmpty();
    }

    @Test
    void paginatesSortedResults() {
        Page<Incident> result = findAll(null, null, null, null, null, null, 1, 2);

        assertThat(result.getContent()).extracting(Incident::getId)
                .containsExactly(20_002L, 20_001L);
        assertThat(result.getTotalElements()).isEqualTo(5);
        assertThat(result.getTotalPages()).isEqualTo(3);
        assertThat(result.hasNext()).isTrue();
        assertThat(result.hasPrevious()).isTrue();
    }

    @Test
    void loadsAllToOneAssociationsWithoutAdditionalQueries() {
        entityManager.flush();
        entityManager.clear();
        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();

        Page<Incident> result = findAll(null, null, null, null, null, null, 0, 2);
        result.getContent().forEach(incident -> {
            incident.getAffectedService().getName();
            incident.getReporter().getDisplayName();
            if (incident.getAssignee() != null) {
                incident.getAssignee().getDisplayName();
            }
            if (incident.getResponsibleTeam() != null) {
                incident.getResponsibleTeam().getName();
            }
        });

        assertThat(statistics.getPrepareStatementCount()).isEqualTo(2);
    }

    @Test
    void supportsNullResponsibleTeam() {
        Incident incident = findAll(
                null, null, null, null, APPLICATION_SERVICE_ID, null, 0, 10
        ).getContent().getFirst();

        assertThat(incident.getResponsibleTeam()).isNull();
    }

    private Page<Incident> findAll(
            IncidentStatus status,
            IncidentPriority priority,
            IncidentSeverity severity,
            IncidentSource source,
            Long affectedServiceId,
            Long responsibleTeamId,
            int page,
            int size
    ) {
        return incidentRepository.findAll(
                IncidentSpecifications.withFilters(
                        status,
                        priority,
                        severity,
                        source,
                        affectedServiceId,
                        responsibleTeamId
                ),
                pageRequest(page, size)
        );
    }

    private static Pageable pageRequest(int page, int size) {
        return PageRequest.of(
                page,
                size,
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))
        );
    }

    private void insertUser(long id, String email, String displayName) {
        Instant now = Instant.parse("2026-08-01T10:00:00Z");
        jdbcTemplate.update(
                """
                        insert into users (id, email, display_name, active, created_at, updated_at)
                        values (?, ?, ?, true, ?, ?)
                        """,
                id, email, displayName, Timestamp.from(now), Timestamp.from(now)
        );
    }

    private void insertTeam(long id, String name, String code) {
        Instant now = Instant.parse("2026-08-01T10:00:00Z");
        jdbcTemplate.update(
                """
                        insert into teams (id, name, code, active, created_at, updated_at)
                        values (?, ?, ?, true, ?, ?)
                        """,
                id, name, code, Timestamp.from(now), Timestamp.from(now)
        );
    }

    private void insertBusinessService(long id, String code, String name) {
        Instant now = Instant.parse("2026-08-01T10:00:00Z");
        jdbcTemplate.update(
                """
                        insert into business_services (
                            id, code, name, description, owner_team_id, tier, active, created_at, updated_at
                        ) values (?, ?, ?, ?, ?, 'TIER_2', true, ?, ?)
                        """,
                id, code, name, name + " service", TEAM_ID, Timestamp.from(now), Timestamp.from(now)
        );
    }

    private void insertIncident(
            long id,
            String title,
            long affectedServiceId,
            String source,
            String priority,
            String severity,
            String status,
            Long assigneeId,
            Long responsibleTeamId,
            String createdAt
    ) {
        Timestamp timestamp = Timestamp.from(Instant.parse(createdAt));
        jdbcTemplate.update(
                """
                        insert into incidents (
                            id, title, description, affected_service_id, source, priority, severity, status,
                            reporter_id, assignee_id, responsible_team_id, created_at, updated_at
                        )
                        values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                id, title, "Description for " + title, affectedServiceId, source, priority, severity, status,
                REPORTER_ID, assigneeId, responsibleTeamId, timestamp, timestamp
        );
    }
}
