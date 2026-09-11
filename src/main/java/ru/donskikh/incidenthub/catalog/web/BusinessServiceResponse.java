package ru.donskikh.incidenthub.catalog.web;

import ru.donskikh.incidenthub.catalog.ServiceTier;

import java.time.Instant;
import java.util.List;

public record BusinessServiceResponse(
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
        List<DirectServiceDependencyResponse> dependencies
) {
}
