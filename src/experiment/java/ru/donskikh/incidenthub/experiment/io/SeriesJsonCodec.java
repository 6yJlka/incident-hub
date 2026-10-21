package ru.donskikh.incidenthub.experiment.io;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import ru.donskikh.incidenthub.experiment.series.*;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Set;

public final class SeriesJsonCodec {
    private static final JsonMapper MAPPER = JsonMapper.builder().addMixIn(CorrelatorSpec.class, SpecTypes.class)
            .addMixIn(Duration.class, IsoTime.class).enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .enable(DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES).enable(DeserializationFeature.FAIL_ON_NULL_CREATOR_PROPERTIES)
            .enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES).build();
    public ExperimentSeriesConfig read(byte[] json) {
        var document = MAPPER.readTree(Objects.requireNonNull(json, "json must not be null"));
        if (!(document instanceof ObjectNode object)) throw new IllegalArgumentException("Expected a series configuration object");
        var names = Set.of("formatVersion", "graph", "distortion", "spuriousEdgeType", "scenario", "correlator", "scenarioSeeds");
        object.properties().forEach(entry -> {
            if (!names.contains(entry.getKey())) throw new IllegalArgumentException("Unknown series field: " + entry.getKey());
        });
        if (!object.path("formatVersion").isIntegralNumber() || !object.path("formatVersion").canConvertToInt() || object.path("formatVersion").asInt() != 1
                || !object.path("scenarioSeeds").isArray()) throw new IllegalArgumentException("Invalid series version or seeds");
        var spec = MAPPER.treeToValue(object.get("correlator"), CorrelatorSpec.class);
        var seeds = new ArrayList<Long>();
        object.path("scenarioSeeds").forEach(seed -> {
            if (!seed.isIntegralNumber() || !seed.canConvertToLong()) throw new IllegalArgumentException("Seeds must be long integers");
            seeds.add(seed.asLong());
        });
        var generation = object.deepCopy();
        generation.remove("formatVersion"); generation.remove("correlator"); generation.remove("scenarioSeeds");
        generation.put("scenarioSeed", 0);
        var parameters = new ScenarioJsonCodec().readParameters(MAPPER.writeValueAsBytes(generation));
        return new ExperimentSeriesConfig(1, parameters.graph(), parameters.distortion(), parameters.spuriousEdgeType(), parameters.scenario(), spec, seeds);
    }
    public String parameters(ExperimentSeriesConfig config, long seed) {
        var document = (ObjectNode) MAPPER.readTree(new ScenarioJsonCodec().writeParameters(config.generation(seed)));
        document.set("correlator", MAPPER.valueToTree(config.correlator()));
        document.put("formatVersion", 1); document.put("modelVersion", "1");
        return document.toString();
    }
    @JsonFormat(shape = JsonFormat.Shape.STRING) private interface IsoTime {}
    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
    @JsonSubTypes({@JsonSubTypes.Type(value = CorrelatorSpec.TimeWindow.class, name = "TIME_WINDOW"),
            @JsonSubTypes.Type(value = CorrelatorSpec.NaiveGraphRoot.class, name = "NAIVE_GRAPH_ROOT")})
    private interface SpecTypes {}
}
