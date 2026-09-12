package ru.donskikh.incidenthub.catalog.web;

import io.swagger.v3.oas.annotations.media.Schema;

import ru.donskikh.incidenthub.catalog.DependencyType;

import java.time.Instant;

public record AddServiceDependencyResponse(
        @Schema(description = "Generated dependency relationship identifier", example = "100") Long relationshipId,
        @Schema(description = "Service that depends on another service", example = "42") Long dependentServiceId,
        @Schema(description = "Service required by the dependent service", example = "41") Long dependencyServiceId,
        @Schema(description = "Dependency interaction type", example = "SYNC") DependencyType type,
        @Schema(description = "Relationship creation timestamp", example = "2026-09-01T10:00:00Z") Instant createdAt
) {
}
