package ru.donskikh.incidenthub.catalog.web;

import ru.donskikh.incidenthub.catalog.DependencyType;

import java.time.Instant;

public record AddServiceDependencyResponse(
        Long relationshipId,
        Long dependentServiceId,
        Long dependencyServiceId,
        DependencyType type,
        Instant createdAt
) {
}
