package ru.donskikh.incidenthub;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import ru.donskikh.incidenthub.catalog.application.AffectedServiceItem;
import ru.donskikh.incidenthub.catalog.application.GetAffectedServicesQuery;
import ru.donskikh.incidenthub.catalog.application.GetAffectedServicesResult;
import ru.donskikh.incidenthub.catalog.application.GetAffectedServicesService;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.datasource.hikari.schema=incident_hub_demo_test",
        "spring.flyway.schemas=incident_hub_demo_test",
        "spring.flyway.default-schema=incident_hub_demo_test"
})
@ActiveProfiles("demo")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DemoDataMigrationIntegrationTest extends PostgreSQLIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private GetAffectedServicesService affectedServicesService;

    @Test
    void appliesV7AndLoadsAConsistentDemoDataset() {
        assertThat(appliedVersions()).containsExactly("1", "2", "3", "4", "5", "6", "7");
        assertThat(count("teams")).isEqualTo(4);
        assertThat(count("users")).isEqualTo(6);
        assertThat(count("business_services")).isEqualTo(10);
        assertThat(count("service_dependencies")).isEqualTo(10);
        assertThat(count("incidents")).isEqualTo(12);
        assertThat(count("incident_audit_events")).isEqualTo(39);

        assertThat(jdbcTemplate.queryForObject(
                "select status from incidents where title = 'Billing event backlog reopened'",
                String.class
        )).isEqualTo("IN_PROGRESS");
        assertThat(jdbcTemplate.queryForList(
                """
                        select event_type
                        from incident_audit_events
                        where incident_id = (
                            select id from incidents where title = 'Billing event backlog reopened'
                        )
                        order by created_at, id
                        """,
                String.class
        )).containsExactly("CREATED", "ASSIGNED", "STARTED", "RESOLVED", "REOPENED");
    }

    @Test
    void deduplicatesDiamondAtMinimumDepthAndKeepsThreeTraversalLevels() {
        long coreDatabaseId = jdbcTemplate.queryForObject(
                "select id from business_services where code = 'CORE_DATABASE'",
                Long.class
        );

        GetAffectedServicesResult result = affectedServicesService.get(
                new GetAffectedServicesQuery(coreDatabaseId, 3)
        );

        assertThat(result.items()).filteredOn(item -> item.code().equals("CUSTOMER_PORTAL"))
                .singleElement()
                .extracting(AffectedServiceItem::depth)
                .isEqualTo(2);
        assertThat(result.items()).extracting(AffectedServiceItem::depth).contains(1, 2, 3);
        assertThat(result.items()).extracting(AffectedServiceItem::code)
                .contains("IDENTITY_API", "PAYMENT_API", "API_GATEWAY", "BILLING_WORKER",
                        "CUSTOMER_PORTAL", "SUPPORT_DESK", "ANALYTICS");
    }

    @AfterAll
    void dropTestSchema() {
        jdbcTemplate.execute("drop schema if exists incident_hub_demo_test cascade");
    }

    private List<String> appliedVersions() {
        return jdbcTemplate.queryForList(
                """
                        select version
                        from flyway_schema_history
                        where success and version is not null
                        order by installed_rank
                        """,
                String.class
        );
    }

    private long count(String table) {
        return jdbcTemplate.queryForObject("select count(*) from " + table, Long.class);
    }
}
