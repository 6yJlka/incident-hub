package ru.donskikh.incidenthub.experiment.graph;

import ru.donskikh.incidenthub.catalog.DependencyType;

import java.util.Objects;

public record DistortGraphCommand(ServiceGraph trueGraph, GraphDistortion distortion, DependencyType spuriousEdgeType) {
    public DistortGraphCommand {
        Objects.requireNonNull(trueGraph, "trueGraph must not be null");
        Objects.requireNonNull(distortion, "distortion must not be null");
        Objects.requireNonNull(spuriousEdgeType, "spuriousEdgeType must not be null");
    }
}
