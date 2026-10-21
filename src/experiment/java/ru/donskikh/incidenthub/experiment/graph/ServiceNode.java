package ru.donskikh.incidenthub.experiment.graph;

import ru.donskikh.incidenthub.catalog.ServiceTier;

import java.util.Objects;

public record ServiceNode(long id, String code, String name, ServiceTier tier) {
    public ServiceNode {
        if (id <= 0) throw new IllegalArgumentException("Service id must be positive");
        if (code == null || code.isBlank() || name == null || name.isBlank()) {
            throw new IllegalArgumentException("Service code and name must not be blank");
        }
        Objects.requireNonNull(tier, "tier must not be null");
    }
}
