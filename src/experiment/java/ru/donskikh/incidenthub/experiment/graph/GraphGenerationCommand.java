package ru.donskikh.incidenthub.experiment.graph;

import ru.donskikh.incidenthub.catalog.DependencyType;
import ru.donskikh.incidenthub.catalog.ServiceTier;

import java.util.Objects;

public record GraphGenerationCommand(GraphSpec spec, long seed, ServiceTier tier, DependencyType edgeType) {
    public GraphGenerationCommand {
        Objects.requireNonNull(spec, "spec must not be null");
        Objects.requireNonNull(tier, "tier must not be null");
        Objects.requireNonNull(edgeType, "edgeType must not be null");
    }
}
