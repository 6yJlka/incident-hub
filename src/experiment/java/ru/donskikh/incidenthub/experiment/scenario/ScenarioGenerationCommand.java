package ru.donskikh.incidenthub.experiment.scenario;

import ru.donskikh.incidenthub.experiment.graph.ServiceGraph;

import java.util.Objects;

public record ScenarioGenerationCommand(ServiceGraph trueGraph, ScenarioParameters parameters, long seed) {
    public ScenarioGenerationCommand {
        Objects.requireNonNull(trueGraph, "trueGraph must not be null");
        Objects.requireNonNull(parameters, "parameters must not be null");
    }
}
