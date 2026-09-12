package ru.donskikh.incidenthub.incident.web;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AssignIncidentRequest(
        @Schema(description = "Active user who will handle the incident", example = "21")
        @NotNull @Positive Long assigneeId
) {
}
