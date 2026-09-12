package ru.donskikh.incidenthub.catalog.web;

import io.swagger.v3.oas.annotations.media.Schema;

import ru.donskikh.incidenthub.catalog.ServiceTier;

import java.time.Instant;

public record BusinessServiceListItemResponse(
        @Schema(description = "Service identifier", example = "42") Long id,
        @Schema(description = "Normalized service code", example = "PAYMENT_API") String code,
        @Schema(description = "Service name", example = "Payment API") String name,
        @Schema(description = "Owner team identifier", example = "12") Long ownerTeamId,
        @Schema(description = "Owner team name", example = "Payments Platform") String ownerTeamName,
        @Schema(description = "Owner team code", example = "PAYMENTS") String ownerTeamCode,
        @Schema(description = "Business criticality tier", example = "TIER_1") ServiceTier tier,
        @Schema(description = "Whether the service is active", example = "true") boolean active,
        @Schema(description = "Creation timestamp", example = "2026-09-01T10:00:00Z") Instant createdAt,
        @Schema(description = "Last update timestamp", example = "2026-09-02T11:00:00Z") Instant updatedAt
) {
}
