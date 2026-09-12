package ru.donskikh.incidenthub.catalog.web;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import ru.donskikh.incidenthub.catalog.ServiceTier;

public record CreateBusinessServiceRequest(
        @Schema(description = "Unique service code; stored trimmed and uppercase", example = "PAYMENT_API")
        @NotBlank @Size(max = 50) String code,
        @Schema(description = "Human-readable service name", example = "Payment API")
        @NotBlank @Size(max = 150) String name,
        @Schema(description = "Service purpose and responsibility", example = "Processes card authorization requests")
        @NotBlank String description,
        @Schema(description = "Identifier of the team that owns the service", example = "12")
        @NotNull @Positive Long ownerTeamId,
        @Schema(description = "Business criticality tier; TIER_1 is the most critical", example = "TIER_1",
                allowableValues = {"TIER_1", "TIER_2", "TIER_3"})
        @NotNull ServiceTier tier
) {
}
