package ru.donskikh.incidenthub.experiment.series;

import ru.donskikh.incidenthub.catalog.DependencyType;
import ru.donskikh.incidenthub.experiment.GenerationParameters;
import ru.donskikh.incidenthub.experiment.graph.GraphGenerationCommand;
import ru.donskikh.incidenthub.experiment.graph.GraphDistortion;
import ru.donskikh.incidenthub.experiment.scenario.ScenarioParameters;
import java.util.List;
import java.util.Objects;
import java.util.TreeSet;

public record ExperimentSeriesConfig(int formatVersion, GraphGenerationCommand graph, GraphDistortion distortion,
        DependencyType spuriousEdgeType, ScenarioParameters scenario, CorrelatorSpec correlator, List<Long> scenarioSeeds) {
    public ExperimentSeriesConfig {
        if (formatVersion != 1) throw new IllegalArgumentException("Unsupported series configuration version");
        new GenerationParameters(graph, distortion, spuriousEdgeType, scenario, 0);
        Objects.requireNonNull(correlator, "correlator must not be null");
        scenarioSeeds = List.copyOf(Objects.requireNonNull(scenarioSeeds, "scenarioSeeds must not be null"));
        if (scenarioSeeds.isEmpty() || new TreeSet<>(scenarioSeeds).size() != scenarioSeeds.size()) throw new IllegalArgumentException("Seeds must be nonempty and unique");
    }
    public GenerationParameters generation(long seed) { return new GenerationParameters(graph, distortion, spuriousEdgeType, scenario, seed); }
}
