package ru.donskikh.incidenthub.catalog;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import ru.donskikh.incidenthub.team.Team;
import ru.donskikh.incidenthub.team.TeamRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class BusinessServiceRepositoryIntegrationTest {

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
        jdbcTemplate.update("delete from service_dependencies");
        jdbcTemplate.update("delete from business_services");
        jdbcTemplate.update("delete from incidents");
        jdbcTemplate.update("delete from teams");
    }

    @Test
    void persistsAndLoadsBusinessServiceWithRelationshipsAndTimestamps() {
        Team ownerTeam = teamRepository.saveAndFlush(new Team("Platform", "PLATFORM"));
        BusinessService saved = businessServiceRepository.saveAndFlush(new BusinessService(
                "  billing-api  ",
                "  Billing API  ",
                "  Processes payments  ",
                ownerTeam,
                ServiceTier.TIER_1
        ));
        Long serviceId = saved.getId();
        entityManager.clear();

        BusinessService loaded = businessServiceRepository.findById(serviceId).orElseThrow();

        assertThat(loaded.getCode()).isEqualTo("BILLING-API");
        assertThat(loaded.getName()).isEqualTo("Billing API");
        assertThat(loaded.getDescription()).isEqualTo("Processes payments");
        assertThat(loaded.getOwnerTeam().getId()).isEqualTo(ownerTeam.getId());
        assertThat(loaded.getTier()).isEqualTo(ServiceTier.TIER_1);
        assertThat(loaded.isActive()).isTrue();
        assertThat(loaded.getCreatedAt()).isNotNull();
        assertThat(loaded.getUpdatedAt()).isNotNull();
        assertThat(jdbcTemplate.queryForObject(
                "select tier from business_services where id = ?",
                String.class,
                serviceId
        )).isEqualTo("TIER_1");
    }

    @Test
    void enforcesCaseInsensitiveTrimmedCodeUniqueness() {
        Team ownerTeam = teamRepository.saveAndFlush(new Team("Platform", "PLATFORM"));
        businessServiceRepository.saveAndFlush(new BusinessService(
                "BILLING-API",
                "Billing API",
                "Processes payments",
                ownerTeam,
                ServiceTier.TIER_1
        ));

        assertThatThrownBy(() -> jdbcTemplate.update(
                """
                        insert into business_services (code, name, description, owner_team_id, tier)
                        values (' billing-api ', 'Other Billing API', 'Other service', ?, 'TIER_2')
                        """,
                ownerTeam.getId()
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void enforcesBlankCodeConstraint() {
        Team ownerTeam = teamRepository.saveAndFlush(new Team("Platform", "PLATFORM"));

        assertThatThrownBy(() -> jdbcTemplate.update(
                """
                        insert into business_services (code, name, description, owner_team_id, tier)
                        values ('   ', 'Billing API', 'Processes payments', ?, 'TIER_1')
                        """,
                ownerTeam.getId()
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void enforcesBlankNameConstraint() {
        Team ownerTeam = teamRepository.saveAndFlush(new Team("Platform", "PLATFORM"));

        assertThatThrownBy(() -> jdbcTemplate.update(
                """
                        insert into business_services (code, name, description, owner_team_id, tier)
                        values ('BILLING-API', '   ', 'Processes payments', ?, 'TIER_1')
                        """,
                ownerTeam.getId()
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void enforcesBlankDescriptionConstraint() {
        Team ownerTeam = teamRepository.saveAndFlush(new Team("Platform", "PLATFORM"));

        assertThatThrownBy(() -> jdbcTemplate.update(
                """
                        insert into business_services (code, name, description, owner_team_id, tier)
                        values ('BILLING-API', 'Billing API', '   ', ?, 'TIER_1')
                        """,
                ownerTeam.getId()
        )).isInstanceOf(DataIntegrityViolationException.class);
    }
}
