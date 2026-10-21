package ru.donskikh.incidenthub.experiment.io;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import ru.donskikh.incidenthub.experiment.GeneratedScenario;
import ru.donskikh.incidenthub.experiment.GenerationParameters;
import ru.donskikh.incidenthub.experiment.graph.GraphSpec;
import ru.donskikh.incidenthub.experiment.graph.ServiceGraph;
import ru.donskikh.incidenthub.experiment.scenario.DelayDistribution;
import ru.donskikh.incidenthub.experiment.scenario.FailurePlan;
import ru.donskikh.incidenthub.experiment.scenario.TruthLabel;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;
import ru.donskikh.incidenthub.experiment.scenario.ScenarioSeeds;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

public final class ScenarioJsonCodec {
    private static final JsonMapper MAPPER = JsonMapper.builder()
            .addMixIn(GraphSpec.class, GraphTypes.class)
            .addMixIn(DelayDistribution.class, DelayTypes.class)
            .addMixIn(FailurePlan.class, FailureTypes.class)
            .addMixIn(TruthLabel.class, TruthTypes.class)
            .addMixIn(Duration.class, IsoTime.class)
            .addMixIn(Instant.class, IsoTime.class)
            .enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
            .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
            .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .enable(DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES)
            .enable(DeserializationFeature.FAIL_ON_NULL_CREATOR_PROPERTIES)
            .enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES).build();

    public byte[] write(GeneratedScenario scenario) {
        return bytes(Objects.requireNonNull(scenario, "scenario must not be null"));
    }

    public GeneratedScenario read(byte[] json) {
        JsonNode document = MAPPER.readTree(Objects.requireNonNull(json, "json must not be null"));
        JsonNode parameters = document.path("parameters");
        applyDetectionDefaults(parameters);
        if (document.path("randomSeeds") instanceof ObjectNode seeds && !seeds.has("detection")
                && parameters.path("scenarioSeed").isIntegralNumber()) {
            seeds.put("detection", ScenarioSeeds.from(parameters.path("scenarioSeed").asLong()).detection());
        }
        return MAPPER.treeToValue(document, GeneratedScenario.class);
    }

    public byte[] writeParameters(GenerationParameters parameters) {
        return bytes(Objects.requireNonNull(parameters, "parameters must not be null"));
    }

    public GenerationParameters readParameters(byte[] json) {
        JsonNode document = MAPPER.readTree(Objects.requireNonNull(json, "json must not be null"));
        applyDetectionDefaults(document);
        return MAPPER.treeToValue(document, GenerationParameters.class);
    }

    public ServiceGraph readGraph(byte[] json) {
        return MAPPER.readValue(Objects.requireNonNull(json, "json must not be null"), ServiceGraph.class);
    }

    private void applyDetectionDefaults(JsonNode parameters) {
        if (parameters.path("scenario").path("detection") instanceof ObjectNode detection) {
            if (!detection.has("detectionProbability")) detection.put("detectionProbability", 1.0);
            if (!detection.has("probabilityOverrides")) detection.set("probabilityOverrides", MAPPER.createObjectNode());
        }
    }

    private byte[] bytes(Object value) {
        return (MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(value).replace("\r\n", "\n") + "\n")
                .getBytes(StandardCharsets.UTF_8);
    }

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private interface IsoTime {}

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
    @JsonSubTypes({
            @JsonSubTypes.Type(value = GraphSpec.Demo.class, name = "DEMO"),
            @JsonSubTypes.Type(value = GraphSpec.Tree.class, name = "TREE"),
            @JsonSubTypes.Type(value = GraphSpec.Layered.class, name = "LAYERED"),
            @JsonSubTypes.Type(value = GraphSpec.Diamonds.class, name = "DIAMONDS"),
            @JsonSubTypes.Type(value = GraphSpec.Cyclic.class, name = "CYCLIC")
    })
    private interface GraphTypes {}

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
    @JsonSubTypes({
            @JsonSubTypes.Type(value = DelayDistribution.Fixed.class, name = "FIXED"),
            @JsonSubTypes.Type(value = DelayDistribution.Uniform.class, name = "UNIFORM")
    })
    private interface DelayTypes {}

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
    @JsonSubTypes({
            @JsonSubTypes.Type(value = FailurePlan.Explicit.class, name = "EXPLICIT"),
            @JsonSubTypes.Type(value = FailurePlan.Selected.class, name = "SELECTED")
    })
    private interface FailureTypes {}

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "kind")
    @JsonSubTypes({
            @JsonSubTypes.Type(value = TruthLabel.FailureReference.class, name = "FAILURE"),
            @JsonSubTypes.Type(value = TruthLabel.Noise.class, name = "NOISE")
    })
    private interface TruthTypes {}
}
