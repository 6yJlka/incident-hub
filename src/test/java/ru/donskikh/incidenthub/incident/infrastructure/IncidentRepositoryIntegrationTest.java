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
import ru.donskikh.incidenthub.incident.IncidentStatus;

import java.sql.Timestamp;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@Transactional
class IncidentRepositoryIntegrationTest {

    private static final long REPORTER_ID = 10_001L;
    private static final long ASSIGNEE_ID = 10_002L;

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
        jdbcTemplate.update("delete from incidents");
        jdbcTemplate.update("delete from users");

        insertUser(REPORTER_ID, "reporter-list@example.com", "List Reporter");
        insertUser(ASSIGNEE_ID, "assignee-list@example.com", "List Assignee");

        insertIncident(20_001L, "Older open", "Hardware", "HIGH", "OPEN", null,
                "2026-08-01T12:00:00Z");
        insertIncident(20_002L, "Newer id at same time", "Hardware", "HIGH", "OPEN", ASSIGNEE_ID,
                "2026-08-01T12:00:00Z");
        insertIncident(20_003L, "Software closed", "Software", "LOW", "CLOSED", null,
                "2026-08-01T13:00:00Z");
        insertIncident(20_004L, "Oldest open", "Hardware", "LOW", "OPEN", null,
                "2026-08-01T11:00:00Z");
        insertIncident(20_005L, "Newest critical", "Hardware", "CRITICAL", "IN_PROGRESS", ASSIGNEE_ID,
                "2026-08-01T14:00:00Z");
        entityManager.clear();
    }

    @Test
    void findsAllWithStableCreatedAtAndIdDescendingSort() {
        Page<Incident> result = incidentRepository.findAll(
                IncidentSpecifications.withFilters(null, null, null),
                pageRequest(0, 10)
        );

        assertThat(result.getContent())
                .extracting(Incident::getId)
                .containsExactly(20_005L, 20_003L, 20_002L, 20_001L, 20_004L);
    }

    @Test
    void filtersByEachOptionalField() {
        Page<Incident> byStatus = incidentRepository.findAll(
                IncidentSpecifications.withFilters(IncidentStatus.OPEN, null, null),
                pageRequest(0, 10)
        );
        Page<Incident> byPriority = incidentRepository.findAll(
                IncidentSpecifications.withFilters(null, IncidentPriority.LOW, null),
                pageRequest(0, 10)
        );
        Page<Incident> byCategory = incidentRepository.findAll(
                IncidentSpecifications.withFilters(null, null, "Hardware"),
                pageRequest(0, 10)
        );

        assertThat(byStatus.getContent()).extracting(Incident::getId)
                .containsExactly(20_002L, 20_001L, 20_004L);
        assertThat(byPriority.getContent()).extracting(Incident::getId)
                .containsExactly(20_003L, 20_004L);
        assertThat(byCategory.getContent()).extracting(Incident::getId)
                .containsExactly(20_005L, 20_002L, 20_001L, 20_004L);
    }

    @Test
    void combinesFilters() {
        Page<Incident> result = incidentRepository.findAll(
                IncidentSpecifications.withFilters(
                        IncidentStatus.OPEN,
                        IncidentPriority.HIGH,
                        "Hardware"
                ),
                pageRequest(0, 10)
        );

        assertThat(result.getContent()).extracting(Incident::getId)
                .containsExactly(20_002L, 20_001L);
    }

    @Test
    void paginatesSortedResults() {
        Page<Incident> result = incidentRepository.findAll(
                IncidentSpecifications.withFilters(null, null, null),
                pageRequest(1, 2)
        );

        assertThat(result.getContent()).extracting(Incident::getId)
                .containsExactly(20_002L, 20_001L);
        assertThat(result.getTotalElements()).isEqualTo(5);
        assertThat(result.getTotalPages()).isEqualTo(3);
        assertThat(result.hasNext()).isTrue();
        assertThat(result.hasPrevious()).isTrue();
    }

    @Test
    void loadsReporterAndAssigneeWithoutAdditionalQueries() {
        entityManager.flush();
        entityManager.clear();

        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();

        Page<Incident> result = incidentRepository.findAll(
                IncidentSpecifications.withFilters(null, null, null),
                pageRequest(0, 2)
        );
        result.getContent().forEach(incident -> {
            incident.getReporter().getDisplayName();
            if (incident.getAssignee() != null) {
                incident.getAssignee().getDisplayName();
            }
        });

        assertThat(statistics.getPrepareStatementCount()).isEqualTo(2);
    }

    private static Pageable pageRequest(int page, int size) {
        return PageRequest.of(
                page,
                size,
                Sort.by(
                        Sort.Order.desc("createdAt"),
                        Sort.Order.desc("id")
                )
        );
    }

    private void insertUser(long id, String email, String displayName) {
        Instant now = Instant.parse("2026-08-01T10:00:00Z");
        jdbcTemplate.update(
                """
                        insert into users (id, email, display_name, active, created_at, updated_at)
                        values (?, ?, ?, true, ?, ?)
                        """,
                id,
                email,
                displayName,
                Timestamp.from(now),
                Timestamp.from(now)
        );
    }

    private void insertIncident(
            long id,
            String title,
            String category,
            String priority,
            String status,
            Long assigneeId,
            String createdAt
    ) {
        Timestamp timestamp = Timestamp.from(Instant.parse(createdAt));
        jdbcTemplate.update(
                """
                        insert into incidents (
                            id, title, description, category, priority, status,
                            reporter_id, assignee_id, created_at, updated_at
                        )
                        values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                id,
                title,
                "Description for " + title,
                category,
                priority,
                status,
                REPORTER_ID,
                assigneeId,
                timestamp,
                timestamp
        );
    }
}
