package ru.donskikh.incidenthub.catalog.application;

import ru.donskikh.incidenthub.catalog.DependencyType;
import ru.donskikh.incidenthub.catalog.ServiceTier;
import java.util.List;

public record AffectedServiceItem(
        Long id,
        String code,
        String name,
        ServiceTier tier,
        boolean active,
        Long ownerTeamId,
        String ownerTeamName,
        int depth,
        DependencyType dependencyType,
        List<AffectedDependencyItem> dependencies
) {
    public AffectedServiceItem {
        dependencies = List.copyOf(dependencies);
    }

    public AffectedServiceItem(Long id, String code, String name, ServiceTier tier, boolean active,
            Long ownerTeamId, String ownerTeamName, int depth, DependencyType dependencyType) {
        this(id, code, name, tier, active, ownerTeamId, ownerTeamName, depth, dependencyType, List.of());
    }
}
