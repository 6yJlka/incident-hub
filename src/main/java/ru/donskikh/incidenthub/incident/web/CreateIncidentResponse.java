package ru.donskikh.incidenthub.incident.web;

import io.swagger.v3.oas.annotations.media.Schema;

import ru.donskikh.incidenthub.incident.IncidentStatus;

public record CreateIncidentResponse(
        @Schema(description = "Generated incident identifier", example = "73") Long incidentId,
        @Schema(description = "Initial incident status", example = "OPEN") IncidentStatus status
) {
}
