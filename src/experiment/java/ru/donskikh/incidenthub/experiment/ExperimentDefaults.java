package ru.donskikh.incidenthub.experiment;

import ru.donskikh.incidenthub.alert.AlertSeverity;
import ru.donskikh.incidenthub.alert.AlertSignalType;
import ru.donskikh.incidenthub.catalog.DependencyType;
import ru.donskikh.incidenthub.catalog.ServiceTier;
import ru.donskikh.incidenthub.experiment.graph.GraphDistortion;
import ru.donskikh.incidenthub.experiment.graph.GraphGenerationCommand;
import ru.donskikh.incidenthub.experiment.graph.GraphSpec;
import ru.donskikh.incidenthub.experiment.scenario.DelayDistribution;
import ru.donskikh.incidenthub.experiment.scenario.DetectionModel;
import ru.donskikh.incidenthub.experiment.scenario.FailurePlan;
import ru.donskikh.incidenthub.experiment.scenario.NoiseModel;
import ru.donskikh.incidenthub.experiment.scenario.ScenarioParameters;
import ru.donskikh.incidenthub.experiment.scenario.SignalDistribution;
import ru.donskikh.incidenthub.experiment.scenario.SignalModel;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Map;

public final class ExperimentDefaults {
    private ExperimentDefaults() {}

    public static GenerationParameters parameters() {
        var uniformSignals = new SignalDistribution(
                Arrays.stream(AlertSignalType.values()).map(value -> new SignalDistribution.TypeWeight(value, 1)).toList(),
                Arrays.stream(AlertSeverity.values()).map(value -> new SignalDistribution.SeverityWeight(value, 1)).toList());
        return new GenerationParameters(new GraphGenerationCommand(new GraphSpec.Demo(), 42, ServiceTier.TIER_2, DependencyType.SYNC),
                new GraphDistortion(0, 0, 42), DependencyType.SYNC,
                new ScenarioParameters(Instant.parse("2026-01-01T00:00:00Z"), Duration.ofMinutes(10),
                        new FailurePlan.Selected(2, Duration.ZERO, Duration.ofSeconds(10), FailurePlan.RootSelectionMode.PATH_CONNECTED_TO_FIRST),
                        .8, new DelayDistribution.Uniform(Duration.ofSeconds(1), Duration.ofSeconds(5)),
                        new DetectionModel(new DelayDistribution.Uniform(Duration.ZERO, Duration.ofSeconds(30)), Map.of()),
                        new NoiseModel(1), new SignalModel(uniformSignals, uniformSignals, uniformSignals)), 42);
    }
}
