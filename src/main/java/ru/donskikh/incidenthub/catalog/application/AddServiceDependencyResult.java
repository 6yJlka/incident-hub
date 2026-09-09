package ru.donskikh.incidenthub.catalog.application;

import ru.donskikh.incidenthub.catalog.DependencyType;

import java.time.Instant;

public record AddServiceDependencyResult(
        Long relationshipId,
        Long dependentServiceId,
        Long dependencyServiceId,
        DependencyType type,
        Instant createdAt
) {
}
