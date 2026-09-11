package ru.donskikh.incidenthub.incident.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import ru.donskikh.incidenthub.incident.IncidentPriority;
import ru.donskikh.incidenthub.incident.IncidentSeverity;

public record CreateIncidentRequest(
        @NotBlank @Size(max = 255) String title,
        @NotBlank String description,
        @NotNull @Positive Long affectedServiceId,
        @NotNull IncidentPriority priority,
        @NotNull IncidentSeverity severity,
        @NotNull @Positive Long reporterId,
        @Positive Long responsibleTeamId
) {
}
