package ru.donskikh.incidenthub.catalog.application;

import ru.donskikh.incidenthub.catalog.DependencyType;
import ru.donskikh.incidenthub.catalog.ServiceTier;

public record AffectedServiceItem(
        Long id,
        String code,
        String name,
        ServiceTier tier,
        boolean active,
        Long ownerTeamId,
        String ownerTeamName,
        int depth,
        DependencyType dependencyType
) {
}
