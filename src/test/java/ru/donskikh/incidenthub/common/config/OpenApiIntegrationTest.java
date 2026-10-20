package ru.donskikh.incidenthub.common.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jayway.jsonpath.JsonPath;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.JsonMetaSchema;
import com.networknt.schema.NonValidationKeyword;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidatorTypeCode;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.donskikh.incidenthub.PostgreSQLIntegrationTest;
import ru.donskikh.incidenthub.alert.correlation.RootAssessmentStatus;
import ru.donskikh.incidenthub.alertgroup.web.EvidenceResponse;
import ru.donskikh.incidenthub.alertgroup.web.RootAssessmentResponse;
import ru.donskikh.incidenthub.alertgroup.web.RootCandidateResponse;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private tools.jackson.databind.ObjectMapper applicationObjectMapper;

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

        List<String> operationIds = paths.values().stream()
                .flatMap(path -> path.entrySet().stream())
                .filter(entry -> HTTP_METHODS.contains(entry.getKey()))
                .map(entry -> ((Map<?, ?>) entry.getValue()).get("operationId"))
                .map(value -> (String) value)
                .toList();

        assertThat(operationIds).hasSize(operationCount).doesNotHaveDuplicates();
        assertThat(operationIds).allSatisfy(id -> assertThat(id).isNotBlank());
        assertThat(operationIds).containsExactlyInAnyOrder(
                "registerUser", "login", "createUser", "getCurrentUser", "listUsers",
                "createTeam", "listTeams", "createService", "listServices", "getService",
                "getAffectedServices", "addServiceDependency", "removeServiceDependency",
                "createIncident", "listIncidents", "getIncident", "getIncidentHistory",
                "assignIncident", "startIncidentProgress", "resolveIncident", "closeIncident",
                "reopenIncident", "cancelIncident", "acceptAlerts", "listAlertGroups", "getAlertGroup",
                "confirmAlertGroup", "dismissAlertGroup"
        );
        assertThat(operationCount).isEqualTo(28);
        assertThat(JsonPath.<String>read(document,
                "$.paths['/api/v1/alert-groups'].get.responses['200'].content['application/json'].schema['$ref']"))
                .isEqualTo("#/components/schemas/ListAlertGroupsResponse");
        assertThat(JsonPath.<String>read(document,
                "$.paths['/api/v1/alert-groups/{id}'].get.responses['200'].content['application/json'].schema['$ref']"))
                .isEqualTo("#/components/schemas/AlertGroupDetailResponse");
        assertThat(tags).containsExactlyInAnyOrder(
                "Authentication", "Incidents", "Service catalog", "Teams", "Users", "Alerts", "Alert groups"
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
        assertThat(JsonPath.<List<Map<String, List<String>>>>read(document, "$.paths['/api/v1/alerts'].post.security"))
                .containsExactly(Map.of("integrationToken", List.of()));
        assertThat(JsonPath.<String>read(document, "$.components.securitySchemes.integrationToken.name"))
                .isEqualTo("X-Integration-Token");
        assertThat(JsonPath.<String>read(document, "$.components.securitySchemes.integrationToken.type"))
                .isEqualTo("apiKey");
        assertThat(JsonPath.<String>read(document,
                "$.paths['/api/v1/alerts'].post.responses['400'].content['application/problem+json'].schema['$ref']"))
                .isEqualTo("#/components/schemas/ProblemDetail");
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

    @Test
    void matchesCommittedFrontendSnapshot() throws Exception {
        String document = mockMvc.perform(get("/v3/api-docs")
                        .header("Host", "localhost:8080"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String snapshot = Files.readString(Path.of("frontend/openapi/incident-hub.json"));

        JsonNode actual = OBJECT_MAPPER.readTree(document);
        JsonNode expected = OBJECT_MAPPER.readTree(snapshot);

        assertThat(actual).as("OpenAPI snapshot must match the backend contract").isEqualTo(expected);
    }

    @Test
    void parsesAndResolvesGeneratedSpecificationWithoutDiagnostics() throws Exception {
        var result = new OpenAPIV3Parser().readContents(generatedDocument(), null, resolveOptions());
        assertThat(result.getOpenAPI()).isNotNull();
        assertThat(result.getMessages()).as("OpenAPI parser/resolver errors and warnings").isEmpty();
    }

    @Test
    void reportsBrokenReferencesInsteadOfSilentlyAcceptingThem() throws Exception {
        var document = OBJECT_MAPPER.readTree(generatedDocument());
        ((ObjectNode) document.at("/components/schemas/EvidenceResponse/oneOf/0"))
                .put("$ref", "#/components/schemas/MissingEvidence");
        var result = new OpenAPIV3Parser().readContents(document.toString(), null, resolveOptions());
        assertThat(result.getMessages()).isNotEmpty();
    }

    @Test
    void declaresConcreteEvidenceObjectsWithoutRecursiveInheritance() throws Exception {
        assertThat(evidenceResponses().map(Object::getClass).toList())
                .containsExactlyInAnyOrder(EvidenceResponse.class.getPermittedSubclasses());
        assertThat(assessmentResponses().map(RootAssessmentResponse::status).toList())
                .containsExactlyInAnyOrder(RootAssessmentStatus.values());
        var schemas = OBJECT_MAPPER.readTree(generatedDocument()).at("/components/schemas");
        var variants = Map.of(
                "TEMPORAL_PROXIMITY", Map.entry("TemporalProximityEvidence", Set.of("type", "alertId", "distanceToGroup")),
                "EARLIEST_ALERT", Map.entry("EarliestAlertEvidence", Set.of("type", "alertId", "serviceId")),
                "DEPENDENCY_PATH", Map.entry("DependencyPathEvidence", Set.of("type", "serviceIds")),
                "MIGRATED", Map.entry("MigratedEvidence", Set.of("type")));
        var evidence = schemas.get("EvidenceResponse");
        assertThat(evidence.get("oneOf")).hasSize(4);
        assertThat(evidence.at("/discriminator/propertyName").asText()).isEqualTo("type");
        variants.forEach((type, variant) -> {
            String reference = "#/components/schemas/" + variant.getKey();
            assertThat(evidence.at("/discriminator/mapping/" + type).asText()).isEqualTo(reference);
            assertThat(evidence.get("oneOf")).anySatisfy(item -> assertThat(item.get("$ref").asText()).isEqualTo(reference));
            var schema = schemas.get(variant.getKey());
            assertConcreteVariant(schema);
            assertThat(schema.at("/properties/type/enum")).singleElement().satisfies(value -> assertThat(value.asText()).isEqualTo(type));
            Set<String> properties = new java.util.HashSet<>();
            schema.get("properties").fieldNames().forEachRemaining(properties::add);
            assertThat(properties).isEqualTo(variant.getValue());
            assertThat(schema.get("required")).extracting(JsonNode::asText).containsExactlyInAnyOrderElementsOf(properties);
            assertThat(schema.get("additionalProperties").asBoolean()).isFalse();
        });
    }

    @Test
    void declaresJsonForEverySuccessfulResponseBody() throws Exception {
        var paths = OBJECT_MAPPER.readTree(generatedDocument()).get("paths");
        for (JsonNode path : paths) {
            for (String method : HTTP_METHODS) {
                var responses = path.path(method).path("responses");
                var entries = responses.fields();
                while (entries.hasNext()) {
                    var entry = entries.next();
                    var body = entry.getValue().path("content");
                    if (entry.getKey().equals("200")) {
                        assertThat(body.isMissingNode()).as("200 responses describe their JSON body").isFalse();
                    }
                    if (entry.getKey().startsWith("2") && !body.isMissingNode()) {
                        assertThat(body).hasSize(1);
                        assertThat(body.has("*/*")).isFalse();
                        assertThat(body.has("application/json")).isTrue();
                    }
                }
            }
        }
    }

    @Test
    void rejectsThePreviousRecursiveEvidenceSchema() throws Exception {
        var document = (ObjectNode) OBJECT_MAPPER.readTree(generatedDocument());
        var variant = (ObjectNode) document.at("/components/schemas/TemporalProximityEvidence");
        variant.removeAll();
        variant.putArray("allOf").addObject().put("$ref", "#/components/schemas/EvidenceResponse");
        assertThatThrownBy(() -> assertConcreteVariant(variant)).isInstanceOf(AssertionError.class);
    }

    private void assertConcreteVariant(JsonNode schema) {
        assertThat(schema.path("type").asText()).isEqualTo("object");
        assertThat(schema.has("allOf")).isFalse();
        assertThat(schema.has("oneOf")).isFalse();
    }

    static Stream<EvidenceResponse> evidenceResponses() {
        return Stream.of(new EvidenceResponse.TemporalProximity(101, "PT-20S"),
                new EvidenceResponse.EarliestAlert(101, 42),
                new EvidenceResponse.DependencyPath(List.of(42L, 43L)), new EvidenceResponse.Migrated());
    }

    static Stream<RootAssessmentResponse> assessmentResponses() {
        return Stream.of(new RootAssessmentResponse(RootAssessmentStatus.RANKED,
                        List.of(new RootCandidateResponse(42, 1.0, "ROOT", "Root service")), evidenceResponses().toList(), null),
                new RootAssessmentResponse(RootAssessmentStatus.UNDETERMINED, List.of(), List.of(), "INSUFFICIENT_EVIDENCE"));
    }

    @ParameterizedTest
    @MethodSource("evidenceResponses")
    void serializedEvidenceMatchesGeneratedSchema(EvidenceResponse dto) throws Exception {
        assertSerializedContract(dto, "EvidenceResponse");
    }

    @ParameterizedTest
    @MethodSource("assessmentResponses")
    void serializedAssessmentMatchesGeneratedSchema(RootAssessmentResponse dto) throws Exception {
        assertSerializedContract(dto, "RootAssessmentResponse");
    }

    private void assertSerializedContract(Object dto, String schemaName) throws Exception {
        var document = OBJECT_MAPPER.readTree(generatedDocument());
        var validationDocument = OBJECT_MAPPER.createObjectNode();
        validationDocument.put("$schema", "https://json-schema.org/draft/2020-12/schema");
        validationDocument.put("$ref", "#/components/schemas/" + schemaName);
        validationDocument.set("components", document.get("components"));
        // Recognize OpenAPI's discriminator extension and its non-validating documentation keywords.
        var dialect = JsonMetaSchema.builder(JsonMetaSchema.getV202012())
                .keyword(new NonValidationKeyword("components"))
                .keyword(ValidatorTypeCode.DISCRIMINATOR)
                .keyword(new NonValidationKeyword("example")).build();
        JsonSchema schema = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012,
                builder -> builder.metaSchema(dialect)).getSchema(validationDocument);
        // Serialization deliberately uses the application's Jackson 3 mapper, not the validator's Jackson 2 mapper.
        var json = (ObjectNode) OBJECT_MAPPER.readTree(applicationObjectMapper.writeValueAsString(dto));
        assertThat(schema.validate(json)).as("Serialized %s must match generated %s", dto, schemaName).isEmpty();
        var extra = json.deepCopy().put("unexpectedField", true);
        assertThat(schema.validate(extra)).as("Undocumented JSON fields must be rejected").isNotEmpty();
        var fields = json.fieldNames();
        while (fields.hasNext()) {
            String name = fields.next();
            var missing = json.deepCopy();
            missing.remove(name);
            assertThat(schema.validate(missing)).as("Missing required field %s", name).isNotEmpty();
        }
        if (dto instanceof RootAssessmentResponse assessment && assessment.status() == RootAssessmentStatus.RANKED) {
            var nestedExtra = json.deepCopy();
            ((ObjectNode) nestedExtra.at("/candidates/0")).put("unexpectedField", true);
            assertThat(schema.validate(nestedExtra)).as("Nested candidates must reject undocumented fields").isNotEmpty();
        }
    }

    private ParseOptions resolveOptions() {
        var options = new ParseOptions();
        options.setResolve(true);
        options.setResolveFully(true);
        options.setResolveCombinators(false);
        return options;
    }

    private String generatedDocument() throws Exception {
        return mockMvc.perform(get("/v3/api-docs").header("Host", "localhost:8080"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    }
}
