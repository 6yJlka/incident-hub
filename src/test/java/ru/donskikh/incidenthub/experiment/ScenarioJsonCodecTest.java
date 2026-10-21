package ru.donskikh.incidenthub.experiment;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import ru.donskikh.incidenthub.experiment.graph.*;
import ru.donskikh.incidenthub.experiment.io.ScenarioJsonCodec;
import ru.donskikh.incidenthub.experiment.cli.ExperimentGeneratorMain;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import static org.assertj.core.api.Assertions.*;

class ScenarioJsonCodecTest {
    private final ScenarioJsonCodec codec = new ScenarioJsonCodec();
    private final ExperimentGenerator generator = new ExperimentGenerator();

    static Stream<GraphSpec> topologies() {
        return Stream.of(new GraphSpec.Demo(), new GraphSpec.Tree(2, 3), new GraphSpec.Layered(3, 4, .5),
                new GraphSpec.Diamonds(2, 3), new GraphSpec.Cyclic(new GraphSpec.Layered(3, 4, .5), .25));
    }

    @ParameterizedTest
    @MethodSource("topologies")
    void roundTripsEveryTopologyAndProducesIdenticalBytes(GraphSpec spec) {
        var p = ExperimentDefaults.parameters();
        var parameters = new GenerationParameters(ExperimentFixtures.graphCommand(spec, 42), p.distortion(), p.spuriousEdgeType(), p.scenario(), p.scenarioSeed());
        assertThat(codec.readParameters(codec.writeParameters(parameters))).isEqualTo(parameters);
        var scenario = generator.generate(parameters);
        byte[] json = codec.write(scenario);
        assertThat(codec.read(json)).isEqualTo(scenario);
        assertThat(codec.write(codec.read(json))).isEqualTo(json);
        assertThat(codec.write(generator.generate(parameters))).isEqualTo(json);
        assertThat(new String(json, StandardCharsets.UTF_8)).doesNotContain("ru.donskikh", "\r").endsWith("\n");
    }

    @Test
    void differentSeedChangesScenarioAndDistortionDoesNotChangeAlertsOrTruth() {
        var p = ExperimentDefaults.parameters();
        var original = generator.generate(p);
        var reseeded = new GenerationParameters(p.graph(), p.distortion(), p.spuriousEdgeType(), p.scenario(), 43);
        assertThat(codec.write(generator.generate(reseeded))).isNotEqualTo(codec.write(original));
        var distorted = generator.generate(new GenerationParameters(p.graph(), new GraphDistortion(.5, .5, 17),
                p.spuriousEdgeType(), p.scenario(), p.scenarioSeed()));
        assertThat(distorted.observation().observedGraph()).isNotEqualTo(original.observation().observedGraph());
        assertThat(distorted.observation().alerts()).isEqualTo(original.observation().alerts());
        assertThat(distorted.truth()).isEqualTo(original.truth());
    }

    @Test
    void roundTripsExplicitPlanFixedDelayOverridesAndNoiseOnlyTruth() {
        var p = ExperimentDefaults.parameters();
        var s = ExperimentFixtures.parameters();
        var parameters = new GenerationParameters(p.graph(), p.distortion(), p.spuriousEdgeType(), s, 1);
        assertThat(codec.read(codec.write(generator.generate(parameters))).parameters()).isEqualTo(parameters);
        var noiseOnly = new ru.donskikh.incidenthub.experiment.scenario.ScenarioParameters(s.start(), s.injectionPeriod(),
                new ru.donskikh.incidenthub.experiment.scenario.FailurePlan.Explicit(java.util.List.of()), 1, s.propagationDelay(),
                new ru.donskikh.incidenthub.experiment.scenario.DetectionModel(s.detection().defaultDistribution(), java.util.Map.of(1L, s.propagationDelay())),
                new ru.donskikh.incidenthub.experiment.scenario.NoiseModel(2), s.signals());
        var result = generator.generate(new GenerationParameters(p.graph(), p.distortion(), p.spuriousEdgeType(), noiseOnly, 42));
        assertThat(codec.read(codec.write(result))).isEqualTo(result);
        assertThat(result.truth().failures()).isEmpty();
        assertThat(result.truth().byAlertKey().values()).allMatch(label -> label instanceof ru.donskikh.incidenthub.experiment.scenario.TruthLabel.Noise);
    }

    @Test
    void rejectsUnknownVersionsFieldsAndMissingRequiredMetadata() {
        String json = new String(codec.write(generator.generate(ExperimentDefaults.parameters())), StandardCharsets.UTF_8);
        assertThatThrownBy(() -> codec.read(json.replace("\"formatVersion\" : 1", "\"formatVersion\" : 2").getBytes(StandardCharsets.UTF_8))).isInstanceOf(RuntimeException.class);
        assertThatThrownBy(() -> codec.read(json.replaceFirst("\\{", "{\"unexpected\":true,").getBytes(StandardCharsets.UTF_8))).isInstanceOf(RuntimeException.class);
        assertThatThrownBy(() -> codec.read("{}".getBytes(StandardCharsets.UTF_8))).isInstanceOf(RuntimeException.class);
        assertThat(ScenarioObservation.class.getRecordComponents()).extracting(java.lang.reflect.RecordComponent::getName)
                .containsExactly("observedGraph", "alerts");
    }

    @ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"demo", "tree", "layered", "diamonds", "cyclic"})
    void shippedConfigurationsAreValidAndLaunchWithoutDatabase(String name) throws Exception {
        String config = "src/experiment/resources/experiment/configs/" + name + ".json";
        Path output = Path.of("build/experiments/examples/" + name + ".json");
        ExperimentGeneratorMain.main(new String[]{"--config", config, "--output", output.toString()});
        assertThat(codec.read(Files.readAllBytes(output))).isEqualTo(generator.generate(codec.readParameters(Files.readAllBytes(Path.of(config)))));
    }

    @Test
    void rejectsInconsistentTruthAndPreservesSeedDerivationContract() {
        var scenario = generator.generate(ExperimentDefaults.parameters());
        var labels = new java.util.TreeMap<>(scenario.truth().byAlertKey());
        labels.remove(labels.firstKey());
        assertThatIllegalArgumentException().isThrownBy(() -> new ru.donskikh.incidenthub.experiment.scenario.ScenarioSample(
                scenario.observation().alerts(), new ru.donskikh.incidenthub.experiment.scenario.GroundTruth(labels, scenario.truth().failures())));
        assertThatIllegalArgumentException().isThrownBy(() -> new GeneratedScenario(1, "2", scenario.parameters(),
                scenario.randomSeeds(), scenario.trueGraph(), scenario.observation(), scenario.truth()));
        assertThatIllegalArgumentException().isThrownBy(() -> new GeneratedScenario(1, "1", scenario.parameters(),
                ru.donskikh.incidenthub.experiment.scenario.ScenarioSeeds.from(43), scenario.trueGraph(), scenario.observation(), scenario.truth()));
        assertThat(scenario.randomSeeds().failures()).isNotEqualTo(scenario.randomSeeds().noise()).isNotEqualTo(scenario.randomSeeds().signals());
    }

    @Test
    void launcherWritesReproducibleOutputAndRejectsEscapingPaths() throws Exception {
        Path first = Path.of("build/experiments/tests/first.json");
        Path second = Path.of("build/experiments/tests/second.json");
        ExperimentGeneratorMain.main(new String[]{"--output", first.toString()});
        ExperimentGeneratorMain.main(new String[]{"--output", second.toString()});
        assertThat(Files.readAllBytes(first)).isEqualTo(Files.readAllBytes(second));
        assertThat(codec.read(Files.readAllBytes(first))).isEqualTo(generator.generate(ExperimentDefaults.parameters()));
        assertThatIllegalArgumentException().isThrownBy(() -> ExperimentGeneratorMain.main(new String[]{"--output", "build/outside.json"}));
        assertThatIllegalArgumentException().isThrownBy(() -> ExperimentGeneratorMain.main(new String[]{"--output"}));
        assertThatIllegalArgumentException().isThrownBy(() -> ExperimentGeneratorMain.main(new String[]{"--unknown", "value"}));
    }
}
