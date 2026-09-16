package ru.donskikh.incidenthub.common.config;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.donskikh.incidenthub.PostgreSQLIntegrationTest;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class OpenApiIntegrationTest extends PostgreSQLIntegrationTest {

    private static final Set<String> HTTP_METHODS = Set.of(
            "get", "post", "put", "patch", "delete", "head", "options", "trace"
    );

    @Autowired
    private MockMvc mockMvc;

    @Test
    void exposesValidSpecificationForAllControllerOperations() throws Exception {
        String document = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(JsonPath.<String>read(document, "$.openapi")).startsWith("3.");
        assertThat(JsonPath.<String>read(document, "$.info.title")).isEqualTo("IncidentHub API");

        Map<String, Map<String, Object>> paths = JsonPath.read(document, "$.paths");
        int operationCount = paths.values().stream()
                .mapToInt(path -> (int) path.keySet().stream().filter(HTTP_METHODS::contains).count())
                .sum();
        Set<String> tags = paths.values().stream()
                .flatMap(path -> path.entrySet().stream())
                .filter(entry -> HTTP_METHODS.contains(entry.getKey()))
                .map(Map.Entry::getValue)
                .map(operation -> (Map<?, ?>) operation)
                .map(operation -> (List<?>) operation.get("tags"))
                .flatMap(List::stream)
                .map(String.class::cast)
                .collect(java.util.stream.Collectors.toSet());

        assertThat(operationCount).isEqualTo(23);
        assertThat(tags).containsExactlyInAnyOrder(
                "Authentication", "Incidents", "Service catalog", "Teams", "Users"
        );
        assertThat(paths.keySet()).contains(
                "/api/v1/auth/register",
                "/api/v1/auth/login",
                "/api/v1/incidents",
                "/api/v1/incidents/{id}/history",
                "/api/v1/services",
                "/api/v1/services/{id}/affected",
                "/api/v1/teams",
                "/api/v1/users",
                "/api/v1/users/me"
        );
        assertThat(JsonPath.<String>read(
                document,
                "$.paths['/api/v1/users/me'].get.responses['200'].content['application/json'].schema['$ref']"
        )).isEqualTo("#/components/schemas/CurrentUserResponse");
        assertThat(JsonPath.<String>read(document, "$.components.securitySchemes.bearerAuth.type"))
                .isEqualTo("http");
        assertThat(JsonPath.<String>read(document, "$.components.securitySchemes.bearerAuth.scheme"))
                .isEqualTo("bearer");
        assertThat(JsonPath.<String>read(document, "$.components.securitySchemes.bearerAuth.bearerFormat"))
                .isEqualTo("JWT");
        assertThat(JsonPath.<List<Map<String, List<String>>>>read(document, "$.security"))
                .containsExactly(Map.of("bearerAuth", List.of()));
        assertThat(JsonPath.<List<?>>read(document, "$.paths['/api/v1/auth/login'].post.security"))
                .isEmpty();
        assertThat(JsonPath.<String>read(
                document,
                "$.paths['/api/v1/incidents'].get.responses['401'].content['application/problem+json'].schema['$ref']"
        )).isEqualTo("#/components/schemas/ProblemDetail");
        assertThat(JsonPath.<String>read(
                document,
                "$.paths['/api/v1/incidents/{id}/assign'].post.responses['403'].content['application/problem+json'].schema['$ref']"
        )).isEqualTo("#/components/schemas/ProblemDetail");
        assertThat(JsonPath.<String>read(
                document,
                "$.paths['/api/v1/services'].post.responses['403'].content['application/problem+json'].schema['$ref']"
        )).isEqualTo("#/components/schemas/ProblemDetail");
        assertThat(JsonPath.<Map<String, ?>>read(
                document,
                "$.paths['/api/v1/incidents'].get.responses"
        )).doesNotContainKey("403");
        assertThat(JsonPath.<Map<String, ?>>read(
                document,
                "$.paths['/api/v1/auth/register'].post.responses"
        )).doesNotContainKeys("401", "403");
        assertThat(JsonPath.<String>read(
                document,
                "$.components.schemas.IncidentResponse.properties.availableActions.description"
        )).isEqualTo("Lifecycle actions available to the current user in the current status");
        assertThat(JsonPath.<List<String>>read(
                document,
                "$.components.schemas.IncidentResponse.properties.availableActions.example"
        )).containsExactly("ASSIGN", "CANCEL");
        assertThat(JsonPath.<List<String>>read(
                document,
                "$.components.schemas.IncidentResponse.properties.availableActions.items.enum"
        )).containsExactly("ASSIGN", "START", "RESOLVE", "CLOSE", "REOPEN", "CANCEL");
    }

    @Test
    void exposesSwaggerUiAtConfiguredPath() throws Exception {
        mockMvc.perform(get("/swagger-ui.html"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/swagger-ui/index.html"));

        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML));
    }
}
