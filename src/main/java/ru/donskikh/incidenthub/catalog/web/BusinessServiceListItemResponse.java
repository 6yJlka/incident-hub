package ru.donskikh.incidenthub.catalog.web;

import ru.donskikh.incidenthub.catalog.ServiceTier;

import java.time.Instant;

public record BusinessServiceListItemResponse(
        Long id,
        String code,
        String name,
        Long ownerTeamId,
        String ownerTeamName,
        String ownerTeamCode,
        ServiceTier tier,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
}
