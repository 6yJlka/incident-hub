package ru.donskikh.incidenthub.experiment;

import ru.donskikh.incidenthub.catalog.DependencyType;
import ru.donskikh.incidenthub.experiment.graph.GraphDistortion;
import ru.donskikh.incidenthub.experiment.graph.GraphGenerationCommand;
import ru.donskikh.incidenthub.experiment.scenario.ScenarioParameters;

import java.util.Objects;

public record GenerationParameters(GraphGenerationCommand graph, GraphDistortion distortion,
        DependencyType spuriousEdgeType, ScenarioParameters scenario, long scenarioSeed) {
    public GenerationParameters {
        Objects.requireNonNull(graph, "graph must not be null");
        Objects.requireNonNull(distortion, "distortion must not be null");
        Objects.requireNonNull(spuriousEdgeType, "spuriousEdgeType must not be null");
        Objects.requireNonNull(scenario, "scenario must not be null");
    }
}
