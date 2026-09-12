package ru.donskikh.incidenthub;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://127.0.0.1:5433/incident_hub?currentSchema=incident_hub_default_test",
        "spring.flyway.schemas=incident_hub_default_test",
        "spring.flyway.default-schema=incident_hub_default_test"
})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DefaultMigrationIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void doesNotApplyDemoMigrationInDefaultProfile() {
        List<String> versions = jdbcTemplate.queryForList(
                """
                        select version
                        from flyway_schema_history
                        where success and version is not null
                        order by installed_rank
                        """,
                String.class
        );

        assertThat(versions).containsExactly("1", "2", "3", "4", "5", "6");
        assertThat(jdbcTemplate.queryForObject("select count(*) from teams", Long.class)).isZero();
        assertThat(jdbcTemplate.queryForObject("select count(*) from incidents", Long.class)).isZero();
    }

    @AfterAll
    void dropTestSchema() {
        jdbcTemplate.execute("drop schema if exists incident_hub_default_test cascade");
    }
}
