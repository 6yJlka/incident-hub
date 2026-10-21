package ru.donskikh.incidenthub.alert.correlation;

import ru.donskikh.incidenthub.catalog.DependencyType;
import java.util.Objects;

public record DependencyArc(long dependentServiceId, long dependencyServiceId, DependencyType type) {
    public DependencyArc {
        if (dependentServiceId <= 0 || dependencyServiceId <= 0 || dependentServiceId == dependencyServiceId) {
            throw new IllegalArgumentException("Dependency endpoints must be distinct positive ids");
        }
        Objects.requireNonNull(type, "type must not be null");
    }
}
