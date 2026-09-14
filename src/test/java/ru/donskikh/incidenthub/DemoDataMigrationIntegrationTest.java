package ru.donskikh.incidenthub;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import ru.donskikh.incidenthub.catalog.application.AffectedServiceItem;
import ru.donskikh.incidenthub.catalog.application.GetAffectedServicesQuery;
import ru.donskikh.incidenthub.catalog.application.GetAffectedServicesResult;
import ru.donskikh.incidenthub.catalog.application.GetAffectedServicesService;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.hikari.schema=incident_hub_demo_test",
        "spring.flyway.schemas=incident_hub_demo_test",
        "spring.flyway.default-schema=incident_hub_demo_test"
})
@ActiveProfiles("demo")
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DemoDataMigrationIntegrationTest extends PostgreSQLIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private GetAffectedServicesService affectedServicesService;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void appliesDemoMigrationsAndLoadsAConsistentDataset() {
        assertThat(appliedVersions()).containsExactly(
                "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11"
        );
        assertThat(count("teams")).isEqualTo(4);
        assertThat(count("users")).isEqualTo(6);
        assertThat(count("business_services")).isEqualTo(10);
        assertThat(count("service_dependencies")).isEqualTo(10);
        assertThat(count("incidents")).isEqualTo(12);
        assertThat(count("incident_audit_events")).isEqualTo(39);
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from users where password_hash is not null",
                Long.class
        )).isEqualTo(6);
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from users where role = 'ENGINEER'",
                Long.class
        )).isEqualTo(3);
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from users where role = 'ADMIN'",
                Long.class
        )).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from incident_audit_events where actor_id is null",
                Long.class
        )).isZero();
        assertThat(jdbcTemplate.queryForObject(
                """
                        select count(*)
                        from incident_audit_events event
                        join incidents incident on incident.id = event.incident_id
                        where event.event_type = 'CREATED'
                          and event.actor_id <> incident.reporter_id
                        """,
                Long.class
        )).isZero();
        assertThat(jdbcTemplate.queryForObject(
                """
                        select count(*)
                        from incident_audit_events event
                        join users actor on actor.id = event.actor_id
                        where event.event_type <> 'CREATED'
                          and actor.role <> 'ADMIN'
                        """,
                Long.class
        )).isZero();

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
    void logsInAsActiveDemoUser() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "boris.petrov@incidenthub.demo",
                                  "password": "demo1234"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"));
    }

    @Test
    void rejectsLifecycleActionForDemoReporter() throws Exception {
        String loginResponse = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "anna.ivanova@incidenthub.demo",
                                  "password": "demo1234"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String token = JsonPath.read(loginResponse, "$.accessToken");
        long incidentId = jdbcTemplate.queryForObject(
                "select id from incidents where title = 'Card payment authorization failures'",
                Long.class
        );
        long assigneeId = jdbcTemplate.queryForObject(
                "select id from users where email = 'boris.petrov@incidenthub.demo'",
                Long.class
        );

        mockMvc.perform(post("/api/v1/incidents/{id}/assign", incidentId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"assigneeId\":" + assigneeId + "}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.type").value("urn:incident-hub:problem:access-denied"))
                .andExpect(jsonPath("$.detail").value("You do not have permission to perform this action"));
    }

    @Test
    @Transactional
    void recordsAuthenticatedDemoUserForTheCompleteLifecycle() throws Exception {
        String loginResponse = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "boris.petrov@incidenthub.demo",
                                  "password": "demo1234"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String token = JsonPath.read(loginResponse, "$.accessToken");
        long actorId = jdbcTemplate.queryForObject(
                "select id from users where email = 'boris.petrov@incidenthub.demo'",
                Long.class
        );
        long serviceId = jdbcTemplate.queryForObject(
                "select id from business_services where code = 'PAYMENT_API'",
                Long.class
        );

        String createResponse = mockMvc.perform(post("/api/v1/incidents")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Authenticated lifecycle test",
                                  "description": "Created by the current demo user",
                                  "affectedServiceId": %d,
                                  "priority": "HIGH",
                                  "severity": "SEV2"
                                }
                                """.formatted(serviceId)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        Number incidentIdValue = JsonPath.read(createResponse, "$.incidentId");
        long incidentId = incidentIdValue.longValue();

        mockMvc.perform(post("/api/v1/incidents/{id}/assign", incidentId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"assigneeId\":" + actorId + "}"))
                .andExpect(status().isOk());
        performLifecycleAction(token, incidentId, "start");
        performLifecycleAction(token, incidentId, "resolve");
        performLifecycleAction(token, incidentId, "reopen");
        performLifecycleAction(token, incidentId, "resolve");
        performLifecycleAction(token, incidentId, "close");

        mockMvc.perform(get("/api/v1/incidents/{id}", incidentId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reporterId").value(actorId));

        String historyResponse = mockMvc.perform(get("/api/v1/incidents/{id}/history", incidentId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        List<Number> actorIds = JsonPath.read(historyResponse, "$.items[*].actorId");
        List<String> actorNames = JsonPath.read(historyResponse, "$.items[*].actorDisplayName");

        assertThat(actorIds).hasSize(7)
                .extracting(Number::longValue)
                .containsOnly(actorId);
        assertThat(actorNames).hasSize(7).containsOnly("Boris Petrov");
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

    private void performLifecycleAction(String token, long incidentId, String action) throws Exception {
        mockMvc.perform(post("/api/v1/incidents/{id}/{action}", incidentId, action)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk());
    }
}
