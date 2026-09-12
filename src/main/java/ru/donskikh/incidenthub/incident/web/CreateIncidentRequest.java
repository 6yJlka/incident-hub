package ru.donskikh.incidenthub.incident.web;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import ru.donskikh.incidenthub.incident.IncidentPriority;
import ru.donskikh.incidenthub.incident.IncidentSeverity;

public record CreateIncidentRequest(
        @Schema(description = "Concise incident title", example = "Card payment authorization failures")
        @NotBlank @Size(max = 255) String title,
        @Schema(description = "Observed symptoms and operational context",
                example = "Authorization requests fail for one acquiring bank")
        @NotBlank String description,
        @Schema(description = "Identifier of the service experiencing the failure", example = "42")
        @NotNull @Positive Long affectedServiceId,
        @Schema(description = "Work ordering urgency, independent of business impact",
                example = "CRITICAL", allowableValues = {"LOW", "MEDIUM", "HIGH", "CRITICAL"})
        @NotNull IncidentPriority priority,
        @Schema(description = "Business impact level; SEV1 is highest and is independent of priority",
                example = "SEV1", allowableValues = {"SEV1", "SEV2", "SEV3", "SEV4"})
        @NotNull IncidentSeverity severity,
        @Schema(description = "User who reported the incident", example = "20")
        @NotNull @Positive Long reporterId,
        @Schema(description = "Team responsible for resolution; defaults to the affected service owner when omitted",
                example = "12", nullable = true)
        @Positive Long responsibleTeamId
) {
}
