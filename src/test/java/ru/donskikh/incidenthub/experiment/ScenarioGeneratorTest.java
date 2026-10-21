package ru.donskikh.incidenthub.experiment;

import org.junit.jupiter.api.Test;
import ru.donskikh.incidenthub.alert.AlertSeverity;
import ru.donskikh.incidenthub.alert.AlertSignalType;
import ru.donskikh.incidenthub.experiment.scenario.*;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static ru.donskikh.incidenthub.experiment.ExperimentFixtures.*;

class ScenarioGeneratorTest {
    private final ScenarioGenerator generator = new ScenarioGenerator();

    private ScenarioParameters configure(FailurePlan failures, double probability, DelayDistribution detection,
            NoiseModel noise, SignalModel signals) {
        var p = parameters();
        return new ScenarioParameters(p.start(), p.injectionPeriod(), failures, probability, p.propagationDelay(),
                new DetectionModel(detection, Map.of()), noise, signals);
    }

    @Test
    void labelsEveryAlertAndPropagatesOnlyAgainstTrueEdges() {
        var graph = graph(5, edge(2, 1), edge(3, 2), edge(1, 3), edge(4, 2));
        for (long seed = 0; seed < 50; seed++) {
            var p = configure(parameters().failures(), .5, new DelayDistribution.Fixed(Duration.ZERO),
                    new NoiseModel(2), parameters().signals());
            var sample = generator.generate(new ScenarioGenerationCommand(graph, p, seed));
            assertThat(sample.truth().byAlertKey()).hasSize(sample.alerts().size());
            sample.alerts().forEach(alert -> {
                if (sample.truth().byAlertKey().get(alert.externalKey()) instanceof TruthLabel.FailureReference) {
                    assertThat(reachable(graph, 1, true, false)).contains(alert.serviceId());
                }
            });
            assertThat(sample.alerts()).isSortedAccordingTo(ScenarioAlert.ORDER);
        }
    }

    @Test
    void fullPropagationReachesEveryServiceOnceEvenWithZeroDelayCycles() {
        var graph = graph(5, edge(2, 1), edge(3, 2), edge(1, 3), edge(4, 2));
        var sample = generator.generate(new ScenarioGenerationCommand(graph, parameters(), 42));
        assertThat(sample.alerts()).extracting(ScenarioAlert::serviceId).containsExactlyInAnyOrder(1L, 2L, 3L, 4L);
        assertThat(sample.alerts()).extracting(ScenarioAlert::occurredAt).containsOnly(parameters().start());
        assertThat(sample.alerts()).extracting(ScenarioAlert::externalKey).isSorted();
    }

    @Test
    void zeroProbabilityStillProducesRootAlert() {
        var p = configure(parameters().failures(), 0, new DelayDistribution.Fixed(Duration.ZERO), new NoiseModel(0), parameters().signals());
        assertThat(generator.generate(new ScenarioGenerationCommand(graph(2, edge(2, 1)), p, 1)).alerts())
                .extracting(ScenarioAlert::serviceId).containsExactly(1L);
    }

    @Test
    void overlappingFailuresRetainSeparateAlertsAndLabels() {
        var p = configure(new FailurePlan.Explicit(List.of(new Failure("A", 1, parameters().start()),
                new Failure("B", 2, parameters().start()))), 1, new DelayDistribution.Fixed(Duration.ZERO),
                new NoiseModel(0), parameters().signals());
        var sample = generator.generate(new ScenarioGenerationCommand(graph(3, edge(2, 1), edge(3, 2)), p, 42));
        assertThat(sample.alerts()).hasSize(5);
        assertThat(sample.alerts().stream().filter(a -> a.serviceId() == 3)
                .map(a -> sample.truth().byAlertKey().get(a.externalKey())).toList())
                .containsExactlyInAnyOrder(new TruthLabel.FailureReference("A"), new TruthLabel.FailureReference("B"));
    }

    @Test
    void selectedIndependentRootsHaveDirectedPathAndRequestedSpacing() {
        var graph = graph(4, edge(2, 1), edge(3, 2));
        var p = configure(new FailurePlan.Selected(2, Duration.ZERO, Duration.ofSeconds(7),
                FailurePlan.RootSelectionMode.PATH_CONNECTED_TO_FIRST), 1,
                new DelayDistribution.Fixed(Duration.ZERO), new NoiseModel(0), parameters().signals());
        for (long seed = 0; seed < 50; seed++) {
            var failures = generator.generate(new ScenarioGenerationCommand(graph, p, seed)).truth().failures();
            long first = failures.getFirst().rootServiceId();
            long second = failures.getLast().rootServiceId();
            assertThat(first).isNotEqualTo(second);
            var related = reachable(graph, first, true, false);
            related.addAll(reachable(graph, first, false, false));
            assertThat(related).contains(second);
            assertThat(Duration.between(failures.getFirst().startedAt(), failures.getLast().startedAt())).isEqualTo(Duration.ofSeconds(7));
        }
    }

    @Test
    void detectionSpreadCanMakeDependentAlertEarlierThanRoot() {
        var p = configure(parameters().failures(), 1, new DelayDistribution.Uniform(Duration.ZERO, Duration.ofSeconds(30)),
                new NoiseModel(0), parameters().signals());
        int earlier = 0;
        for (long seed = 0; seed < 100; seed++) {
            var alerts = generator.generate(new ScenarioGenerationCommand(graph(2, edge(2, 1)), p, seed)).alerts();
            if (alerts.getFirst().serviceId() == 2 && alerts.getFirst().occurredAt().isBefore(alerts.getLast().occurredAt())) earlier++;
        }
        assertThat(earlier).isBetween(20, 80);
    }

    @Test
    void changingNoiseIntensityDoesNotChangeFailureAlertsIncludingSignalsAndKeys() {
        var base = parameters();
        var noisy = configure(base.failures(), 1, base.detection().defaultDistribution(), new NoiseModel(10), base.signals());
        var a = generator.generate(new ScenarioGenerationCommand(graph(3, edge(2, 1), edge(3, 2)), base, 42));
        var b = generator.generate(new ScenarioGenerationCommand(graph(3, edge(2, 1), edge(3, 2)), noisy, 42));
        assertThat(b.alerts()).hasSizeGreaterThan(a.alerts().size());
        assertThat(b.alerts().stream().filter(alert -> b.truth().byAlertKey().get(alert.externalKey()) instanceof TruthLabel.FailureReference).toList())
                .isEqualTo(a.alerts());
    }

    @Test
    void signalChangesDoNotChangeTimesServicesKeysOrTruth() {
        var original = configure(parameters().failures(), 1, parameters().detection().defaultDistribution(), new NoiseModel(5), parameters().signals());
        var fixed = distribution(AlertSignalType.LATENCY, AlertSeverity.CRITICAL);
        var altered = configure(original.failures(), 1, original.detection().defaultDistribution(), original.noise(), new SignalModel(fixed, fixed, fixed));
        var a = generator.generate(new ScenarioGenerationCommand(graph(2, edge(2, 1)), original, 42));
        var b = generator.generate(new ScenarioGenerationCommand(graph(2, edge(2, 1)), altered, 42));
        assertThat(a.truth()).isEqualTo(b.truth());
        assertThat(a.alerts().stream().map(alert -> List.of(alert.externalKey(), alert.serviceId(), alert.occurredAt())).toList())
                .isEqualTo(b.alerts().stream().map(alert -> List.of(alert.externalKey(), alert.serviceId(), alert.occurredAt())).toList());
        assertThat(b.alerts()).allSatisfy(alert -> {
            assertThat(alert.signalType()).isEqualTo(AlertSignalType.LATENCY);
            assertThat(alert.severity()).isEqualTo(AlertSeverity.CRITICAL);
        });
    }

    @Test
    void samplesSeparateSignalDistributionsForRootPropagationAndNoise() {
        var signals = new SignalModel(distribution(AlertSignalType.AVAILABILITY, AlertSeverity.CRITICAL),
                distribution(AlertSignalType.LATENCY, AlertSeverity.WARNING), distribution(AlertSignalType.SATURATION, AlertSeverity.INFO));
        var p = configure(parameters().failures(), 1, parameters().detection().defaultDistribution(), new NoiseModel(5), signals);
        var sample = generator.generate(new ScenarioGenerationCommand(graph(2, edge(2, 1)), p, 42));
        assertThat(sample.alerts()).anyMatch(a -> sample.truth().byAlertKey().get(a.externalKey()) instanceof TruthLabel.Noise);
        sample.alerts().forEach(alert -> {
            var expected = sample.truth().byAlertKey().get(alert.externalKey()) instanceof TruthLabel.Noise
                    ? signals.noise() : alert.serviceId() == 1 ? signals.root() : signals.propagated();
            assertThat(alert.signalType()).isEqualTo(expected.types().getFirst().value());
            assertThat(alert.severity()).isEqualTo(expected.severities().getFirst().value());
        });
    }

    @Test
    void serviceDetectionOverrideAndLateAlertsAreRetained() {
        var p = parameters();
        var late = new ScenarioParameters(p.start(), p.injectionPeriod(), p.failures(), 1, p.propagationDelay(),
                new DetectionModel(new DelayDistribution.Fixed(Duration.ZERO), Map.of(1L, new DelayDistribution.Fixed(Duration.ofHours(1)))), p.noise(), p.signals());
        assertThat(generator.generate(new ScenarioGenerationCommand(graph(1), late, 1)).alerts().getFirst().occurredAt())
                .isEqualTo(p.start().plus(Duration.ofHours(1)));
    }

    @Test
    void rejectsImpossibleRootsAndInvalidParameters() {
        var p = configure(new FailurePlan.Selected(2, Duration.ZERO, Duration.ZERO, FailurePlan.RootSelectionMode.PATH_CONNECTED_TO_FIRST),
                1, parameters().detection().defaultDistribution(), new NoiseModel(0), parameters().signals());
        assertThatIllegalArgumentException().isThrownBy(() -> generator.generate(new ScenarioGenerationCommand(graph(2), p, 1)));
        assertThatIllegalArgumentException().isThrownBy(() -> new NoiseModel(-1));
        assertThatIllegalArgumentException().isThrownBy(() -> new DelayDistribution.Fixed(Duration.ofNanos(1)));
        assertThatIllegalArgumentException().isThrownBy(() -> new DelayDistribution.Uniform(Duration.ofSeconds(1), Duration.ZERO));
        assertThatIllegalArgumentException().isThrownBy(() -> new SignalDistribution(List.of(), List.of()));
        assertThatNullPointerException().isThrownBy(() -> generator.generate(null));
    }

    @Test
    void propagationProbabilityAndPoissonNoiseHaveExpectedEmpiricalRates() {
        var p = configure(parameters().failures(), .25, parameters().detection().defaultDistribution(), new NoiseModel(2), parameters().signals());
        int propagated = 0;
        int noise = 0;
        for (long seed = 0; seed < 1000; seed++) {
            var sample = generator.generate(new ScenarioGenerationCommand(graph(2, edge(2, 1)), p, seed));
            propagated += (int) sample.alerts().stream().filter(a -> a.serviceId() == 2
                    && sample.truth().byAlertKey().get(a.externalKey()) instanceof TruthLabel.FailureReference).count();
            noise += (int) sample.truth().byAlertKey().values().stream().filter(label -> label instanceof TruthLabel.Noise).count();
        }
        assertThat(propagated).isBetween(200, 300);
        assertThat(noise).isBetween(19000, 21000);
    }

    @Test
    void weightedSignalDistributionUsesWeightsAndRejectsDuplicates() {
        var distribution = new SignalDistribution(List.of(new SignalDistribution.TypeWeight(AlertSignalType.LATENCY, 3),
                new SignalDistribution.TypeWeight(AlertSignalType.AVAILABILITY, 1)),
                List.of(new SignalDistribution.SeverityWeight(AlertSeverity.WARNING, 1)));
        var random = new java.util.Random(42);
        int latency = 0;
        for (int i = 0; i < 10000; i++) {
            var signal = distribution.sample(random);
            if (signal.type() == AlertSignalType.LATENCY) latency++;
            assertThat(signal.severity()).isEqualTo(AlertSeverity.WARNING);
        }
        assertThat(latency).isBetween(7200, 7800);
        assertThatIllegalArgumentException().isThrownBy(() -> new SignalDistribution.TypeWeight(AlertSignalType.LATENCY, 0));
        assertThatIllegalArgumentException().isThrownBy(() -> new SignalDistribution(
                List.of(new SignalDistribution.TypeWeight(AlertSignalType.LATENCY, 1), new SignalDistribution.TypeWeight(AlertSignalType.LATENCY, 2)), distribution.severities()));
    }

    private SignalDistribution distribution(AlertSignalType type, AlertSeverity severity) {
        return new SignalDistribution(List.of(new SignalDistribution.TypeWeight(type, 1)), List.of(new SignalDistribution.SeverityWeight(severity, 1)));
    }
}
