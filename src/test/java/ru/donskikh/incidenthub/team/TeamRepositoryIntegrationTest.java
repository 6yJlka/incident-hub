package ru.donskikh.incidenthub.team;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class TeamRepositoryIntegrationTest {

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
    void persistsAndLoadsTeamWithTimestampsAndNormalizedCode() {
        Team saved = teamRepository.saveAndFlush(new Team("  Platform  ", "  platform  "));
        Long teamId = saved.getId();
        entityManager.clear();

        Team loaded = teamRepository.findById(teamId).orElseThrow();

        assertThat(loaded.getName()).isEqualTo("Platform");
        assertThat(loaded.getCode()).isEqualTo("PLATFORM");
        assertThat(loaded.isActive()).isTrue();
        assertThat(loaded.getCreatedAt()).isNotNull();
        assertThat(loaded.getUpdatedAt()).isNotNull();
    }

    @Test
    void enforcesCaseInsensitiveCodeUniqueness() {
        jdbcTemplate.update("insert into teams (name, code) values ('Platform', 'PLATFORM')");

        assertThatThrownBy(() ->
                jdbcTemplate.update("insert into teams (name, code) values ('Other', ' platform ')")
        ).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void enforcesBlankNameConstraint() {
        assertThatThrownBy(() ->
                jdbcTemplate.update("insert into teams (name, code) values ('   ', 'PLATFORM')")
        ).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void enforcesBlankCodeConstraint() {
        assertThatThrownBy(() ->
                jdbcTemplate.update("insert into teams (name, code) values ('Platform', '   ')")
        ).isInstanceOf(DataIntegrityViolationException.class);
    }
}
