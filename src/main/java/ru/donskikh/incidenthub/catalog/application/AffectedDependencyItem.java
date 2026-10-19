package ru.donskikh.incidenthub.catalog.application;

import ru.donskikh.incidenthub.catalog.DependencyType;
import java.util.Objects;

public record AffectedDependencyItem(Long serviceId, DependencyType type) {
    public AffectedDependencyItem {
        Objects.requireNonNull(serviceId, "serviceId must not be null");
        Objects.requireNonNull(type, "type must not be null");
        if (serviceId <= 0) {
            throw new IllegalArgumentException("serviceId must be positive");
        }
    }
}
