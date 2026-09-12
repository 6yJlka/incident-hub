package ru.donskikh.incidenthub.catalog.web;

import io.swagger.v3.oas.annotations.media.Schema;

import ru.donskikh.incidenthub.catalog.ServiceTier;

public record CreateBusinessServiceResponse(
        @Schema(description = "Generated service identifier", example = "42") Long businessServiceId,
        @Schema(description = "Normalized service code", example = "PAYMENT_API") String code,
        @Schema(description = "Business criticality tier", example = "TIER_1") ServiceTier tier,
        @Schema(description = "Whether the service is active", example = "true") boolean active
) {
}
