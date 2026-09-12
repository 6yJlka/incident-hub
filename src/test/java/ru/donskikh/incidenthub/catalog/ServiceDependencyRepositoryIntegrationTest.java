package ru.donskikh.incidenthub.catalog;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import ru.donskikh.incidenthub.PostgreSQLIntegrationTest;
import ru.donskikh.incidenthub.team.Team;
import ru.donskikh.incidenthub.team.TeamRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class ServiceDependencyRepositoryIntegrationTest extends PostgreSQLIntegrationTest {

    @Autowired
    private ServiceDependencyRepository serviceDependencyRepository;

    @Autowired
    private BusinessServiceRepository businessServiceRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("delete from incident_audit_events");
        jdbcTemplate.update("delete from incidents");
        jdbcTemplate.update("delete from service_dependencies");
        jdbcTemplate.update("delete from business_services");
        jdbcTemplate.update("delete from teams");
    }

    @Test
    void persistsAndLoadsServiceDependency() {
        ServicePair services = createServices();
        ServiceDependency saved = serviceDependencyRepository.saveAndFlush(new ServiceDependency(
                services.dependent(),
                services.dependency(),
                DependencyType.SYNC
        ));
        Long dependencyId = saved.getId();
        entityManager.clear();

        ServiceDependency loaded = serviceDependencyRepository.findById(dependencyId).orElseThrow();

        assertThat(loaded.getDependent().getId()).isEqualTo(services.dependent().getId());
        assertThat(loaded.getDependency().getId()).isEqualTo(services.dependency().getId());
        assertThat(loaded.getType()).isEqualTo(DependencyType.SYNC);
        assertThat(loaded.getCreatedAt()).isNotNull();
        assertThat(jdbcTemplate.queryForObject(
                "select type from service_dependencies where id = ?",
                String.class,
                dependencyId
        )).isEqualTo("SYNC");
    }

    @Test
    void enforcesUniqueDependentAndDependencyPair() {
        ServicePair services = createServices();
        serviceDependencyRepository.saveAndFlush(new ServiceDependency(
                services.dependent(),
                services.dependency(),
                DependencyType.SYNC
        ));

        assertThatThrownBy(() -> jdbcTemplate.update(
                """
                        insert into service_dependencies (dependent_id, dependency_id, type)
                        values (?, ?, 'ASYNC')
                        """,
                services.dependent().getId(),
                services.dependency().getId()
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void enforcesDatabaseSelfReferenceConstraint() {
        ServicePair services = createServices();

        assertThatThrownBy(() -> jdbcTemplate.update(
                """
                        insert into service_dependencies (dependent_id, dependency_id, type)
                        values (?, ?, 'SYNC')
                        """,
                services.dependent().getId(),
                services.dependent().getId()
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    private ServicePair createServices() {
        Team ownerTeam = teamRepository.saveAndFlush(new Team("Platform", "PLATFORM"));
        BusinessService dependent = businessServiceRepository.saveAndFlush(new BusinessService(
                "CHECKOUT",
                "Checkout",
                "Checkout service",
                ownerTeam,
                ServiceTier.TIER_1
        ));
        BusinessService dependency = businessServiceRepository.saveAndFlush(new BusinessService(
                "BILLING",
                "Billing",
                "Billing service",
                ownerTeam,
                ServiceTier.TIER_1
        ));
        return new ServicePair(dependent, dependency);
    }

    private record ServicePair(BusinessService dependent, BusinessService dependency) {
    }
}
