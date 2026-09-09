package ru.donskikh.incidenthub.catalog.application;

import ru.donskikh.incidenthub.catalog.ServiceTier;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record GetBusinessServiceResult(
        Long id,
        String code,
        String name,
        String description,
        Long ownerTeamId,
        String ownerTeamName,
        String ownerTeamCode,
        ServiceTier tier,
        boolean active,
        Instant createdAt,
        Instant updatedAt,
        List<DirectServiceDependencyItem> dependencies
) {

    public GetBusinessServiceResult {
        Objects.requireNonNull(dependencies, "dependencies must not be null");
        dependencies = List.copyOf(dependencies);
    }
}
