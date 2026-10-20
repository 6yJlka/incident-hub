package ru.donskikh.incidenthub.experiment.graph;

import ru.donskikh.incidenthub.catalog.DependencyType;

import java.util.Comparator;
import java.util.Objects;

public record DependencyEdge(long dependentId, long dependencyId, DependencyType type) {
    public static final Comparator<DependencyEdge> ORDER = Comparator.comparingLong(DependencyEdge::dependentId)
            .thenComparingLong(DependencyEdge::dependencyId);

    public DependencyEdge {
        if (dependentId <= 0 || dependencyId <= 0 || dependentId == dependencyId) {
            throw new IllegalArgumentException("Edge endpoints must be distinct positive ids");
        }
        Objects.requireNonNull(type, "type must not be null");
    }
}
