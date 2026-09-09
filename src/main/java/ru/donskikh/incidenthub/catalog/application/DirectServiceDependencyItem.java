package ru.donskikh.incidenthub.catalog.application;

import ru.donskikh.incidenthub.catalog.DependencyType;
import ru.donskikh.incidenthub.catalog.ServiceTier;

import java.time.Instant;

public record DirectServiceDependencyItem(
        Long relationshipId,
        Long serviceId,
        String code,
        String name,
        ServiceTier tier,
        boolean active,
        DependencyType type,
        Instant createdAt
) {
}
