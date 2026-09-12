package ru.donskikh.incidenthub.catalog.web;

import io.swagger.v3.oas.annotations.media.Schema;

import ru.donskikh.incidenthub.catalog.DependencyType;
import ru.donskikh.incidenthub.catalog.ServiceTier;

import java.time.Instant;

public record DirectServiceDependencyResponse(
        @Schema(description = "Dependency relationship identifier", example = "100") Long relationshipId,
        @Schema(description = "Required service identifier", example = "41") Long serviceId,
        @Schema(description = "Required service code", example = "CORE_DATABASE") String code,
        @Schema(description = "Required service name", example = "Core Database") String name,
        @Schema(description = "Required service criticality tier", example = "TIER_1") ServiceTier tier,
        @Schema(description = "Whether the required service is active", example = "true") boolean active,
        @Schema(description = "Dependency interaction type", example = "DATA") DependencyType type,
        @Schema(description = "Relationship creation timestamp", example = "2026-09-01T10:00:00Z") Instant createdAt
) {
}
