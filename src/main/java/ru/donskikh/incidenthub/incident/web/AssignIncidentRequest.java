package ru.donskikh.incidenthub.incident.web;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AssignIncidentRequest(
        @NotNull @Positive Long assigneeId
) {
}
