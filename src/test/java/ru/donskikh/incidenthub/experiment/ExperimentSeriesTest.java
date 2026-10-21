package ru.donskikh.incidenthub.experiment;

import org.junit.jupiter.api.Test;
import ru.donskikh.incidenthub.alert.correlation.*;
import ru.donskikh.incidenthub.experiment.io.*;
import ru.donskikh.incidenthub.experiment.replay.*;
import ru.donskikh.incidenthub.experiment.series.*;
import ru.donskikh.incidenthub.experiment.cli.ExperimentRunnerMain;
import java.nio.file.*;
import java.time.Duration;
import java.util.List;
import static org.assertj.core.api.Assertions.*;

class ExperimentSeriesTest {
    private ExperimentSeriesConfig config(String method) throws Exception {
        return new SeriesJsonCodec().read(Files.readAllBytes(Path.of("src/experiment/resources/experiment/series/" + method + ".json")));
    }
    @Test
    void seriesIsByteReproducibleAndGroupingIsIdenticalForBothBaselines() throws Exception {
        var a = config("time_window");
        var b = config("naive_graph_root");
        var runner = new ExperimentSeriesRunner();
        var first = runner.run(a);
        var second = runner.run(a);
        var naive = runner.run(b);
        var writer = new SeriesCsvWriter();
        assertThat(first.runs()).hasSize(30);
        assertThat(writer.runs(first)).isEqualTo(writer.runs(second));
        assertThat(writer.summary(first)).isEqualTo(writer.summary(second));
        for (int i = 0; i < 30; i++) {
            for (String key : List.of("pair_precision", "pair_recall", "pair_f1", "false_merge_rate", "groups_per_visible_failure", "absorbed_noise_rate")) {
                assertThat(naive.runs().get(i).metrics().rates().get(key)).isEqualTo(first.runs().get(i).metrics().rates().get(key));
            }
            var scenario = new ExperimentGenerator().generate(a.generation(a.scenarioSeeds().get(i)));
            var replay = new ScenarioReplay();
            var left = replay.replay(scenario.observation(), a.correlator().create());
            var right = replay.replay(scenario.observation(), b.correlator().create());
            assertThat(left.groups().stream().map(g -> g.members().stream().map(ReplayMember::externalKey).toList()).toList())
                    .isEqualTo(right.groups().stream().map(g -> g.members().stream().map(ReplayMember::externalKey).toList()).toList());
        }
        var summary = first.summary().get("noise_alerts");
        double mean = first.runs().stream().mapToDouble(run -> run.metrics().counts().get("noise_alerts")).average().orElseThrow();
        double sd = StrictMath.sqrt(first.runs().stream().mapToDouble(run -> Math.pow(run.metrics().counts().get("noise_alerts") - mean, 2)).sum() / 29);
        assertThat(summary.mean()).isCloseTo(mean, within(1e-12));
        assertThat(summary.standardDeviation()).isCloseTo(sd, within(1e-12));
        assertThat(new String(writer.runs(first), java.nio.charset.StandardCharsets.UTF_8)).doesNotContain("NaN", "Infinity", "\r");
    }
    static java.util.stream.Stream<ru.donskikh.incidenthub.experiment.graph.GraphSpec> syntheticGraphs() {
        return java.util.stream.Stream.of(new ru.donskikh.incidenthub.experiment.graph.GraphSpec.Tree(2, 3),
                new ru.donskikh.incidenthub.experiment.graph.GraphSpec.Layered(3, 4, .5),
                new ru.donskikh.incidenthub.experiment.graph.GraphSpec.Diamonds(2, 3),
                new ru.donskikh.incidenthub.experiment.graph.GraphSpec.Cyclic(new ru.donskikh.incidenthub.experiment.graph.GraphSpec.Layered(3, 4, .5), .25));
    }
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.MethodSource("syntheticGraphs")
    void bothBaselinesRunAcrossTopologiesNoiseAndDistortion(ru.donskikh.incidenthub.experiment.graph.GraphSpec topology) throws Exception {
        var original = config("time_window");
        for (double noise : new double[]{0, 3}) {
            for (double distortion : new double[]{0, .25}) {
                var s = original.scenario();
                var scenario = new ru.donskikh.incidenthub.experiment.scenario.ScenarioParameters(s.start(), s.injectionPeriod(), s.failures(),
                        s.propagationProbability(), s.propagationDelay(), s.detection(), new ru.donskikh.incidenthub.experiment.scenario.NoiseModel(noise), s.signals());
                var temporal = new ExperimentSeriesConfig(1, ExperimentFixtures.graphCommand(topology, 42),
                        new ru.donskikh.incidenthub.experiment.graph.GraphDistortion(distortion, distortion, 17), original.spuriousEdgeType(),
                        scenario, original.correlator(), List.of(1L, 2L));
                var naive = new ExperimentSeriesConfig(1, temporal.graph(), temporal.distortion(), temporal.spuriousEdgeType(), scenario,
                        new CorrelatorSpec.NaiveGraphRoot(Duration.ofMinutes(5), NaiveGraphRootParameters.defaults()), temporal.scenarioSeeds());
                var runner = new ExperimentSeriesRunner();
                var left = runner.run(temporal);
                var right = runner.run(naive);
                for (int run = 0; run < 2; run++) {
                    assertThat(left.runs().get(run).metrics().counts().get("alerts")).isPositive();
                    for (String metric : List.of("pair_precision", "pair_recall", "pair_f1", "false_merge_rate", "absorbed_noise_rate", "groups_per_visible_failure")) {
                        assertThat(right.runs().get(run).metrics().rates().get(metric)).isEqualTo(left.runs().get(run).metrics().rates().get(metric));
                    }
                }
            }
        }
    }

    @Test
    void singleSeedAndAllUndefinedMetricsHaveExplicitSummaryAvailability() throws Exception {
        var c = config("time_window");
        var result = new ExperimentSeriesRunner().run(new ExperimentSeriesConfig(1, c.graph(), c.distortion(), c.spuriousEdgeType(), c.scenario(), c.correlator(), List.of(42L)));
        assertThat(result.summary().get("alerts").definedCount()).isEqualTo(1);
        assertThat(result.summary().get("alerts").standardDeviation()).isNull();
        assertThat(result.summary().get("localization_top1").definedCount()).isZero();
        assertThat(result.summary().get("localization_top1").mean()).isNull();
        assertThat(result.summary().get("localization_top1_strict").mean()).isNull();
    }
    @Test
    void launcherWritesBothCsvFilesAndRejectsInvalidConfiguration() throws Exception {
        String directory = "build/experiments/series-test";
        ExperimentRunnerMain.main(new String[]{"--config", "src/experiment/resources/experiment/series/time_window.json", "--output", directory});
        assertThat(Files.readString(Path.of(directory, "runs.csv"))).contains("parameters", "localization_top1_strict", "ranked_coverage");
        assertThat(Files.readAllLines(Path.of(directory, "summary.csv"))).anyMatch(line -> line.contains("localization_a5_strict"));
        assertThatIllegalArgumentException().isThrownBy(() -> ExperimentRunnerMain.main(new String[]{"--config", "unused", "--output", "build/outside"}));
        assertThatIllegalArgumentException().isThrownBy(() -> ExperimentRunnerMain.main(new String[]{}));
        var c = config("time_window");
        assertThatIllegalArgumentException().isThrownBy(() -> new ExperimentSeriesConfig(1, c.graph(), c.distortion(), c.spuriousEdgeType(), c.scenario(), c.correlator(), List.of(1L, 1L)));
        String json = Files.readString(Path.of("src/experiment/resources/experiment/series/time_window.json"));
        assertThatThrownBy(() -> new SeriesJsonCodec().read(json.replace("\"formatVersion\": 1", "\"formatVersion\": 4294967297").getBytes())).isInstanceOf(RuntimeException.class);
        assertThatThrownBy(() -> new SeriesJsonCodec().read(json.replaceFirst("\\{", "{\"unknown\":1,").getBytes())).isInstanceOf(RuntimeException.class);
    }
    @Test
    void replayPreservesSnapshotsAndReplacesRankedWithUndeterminedIncludingSupportedLinks() {
        var observation = new ExperimentGenerator().generate(ExperimentDefaults.parameters()).observation();
        var seen = new java.util.ArrayList<CorrelationContext>();
        AlertCorrelator stub = (alert, context) -> {
            seen.add(context);
            if (context.groups().isEmpty()) return new CorrelationDecision.OpenGroup(new RootAssessment.Ranked(
                    List.of(new RootCandidate(alert.businessServiceId(), 1)), List.of(new Evidence.EarliestAlert(alert.id(), alert.businessServiceId()))),
                    List.of(new Evidence.EarliestAlert(alert.id(), alert.businessServiceId())));
            return new CorrelationDecision.JoinGroup(1, new RootAssessment.Undetermined("INSUFFICIENT_EVIDENCE"), LinkStrength.SUPPORTED,
                    List.of(new Evidence.TemporalProximity(alert.id(), Duration.ZERO), new Evidence.Migrated()));
        };
        var result = new ScenarioReplay().replay(observation, stub);
        assertThat(result.groups()).hasSize(1);
        assertThat(result.groups().getFirst().snapshot().rootAssessment()).isInstanceOf(RootAssessment.Undetermined.class);
        assertThat(result.groups().getFirst().members().getLast().linkEvidence()).hasSize(2);
        assertThat(result.groups().getFirst().members().getLast().linkStrength()).isEqualTo(LinkStrength.SUPPORTED);
        assertThat(seen.get(1).groups().getFirst().members()).hasSize(1);
        assertThat(seen.get(2).groups().getFirst().members()).hasSize(2);
        assertThat(seen.getFirst().graph().edges()).hasSize(observation.observedGraph().edges().size());
        assertThatIllegalStateException().isThrownBy(() -> new ScenarioReplay().replay(observation, (alert, context) ->
                new CorrelationDecision.JoinGroup(99, new RootAssessment.Undetermined("INSUFFICIENT_EVIDENCE"), LinkStrength.TENTATIVE, List.of(new Evidence.Migrated()))));
    }
}
