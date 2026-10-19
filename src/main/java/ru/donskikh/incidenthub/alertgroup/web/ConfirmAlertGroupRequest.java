package ru.donskikh.incidenthub.alertgroup.web;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import ru.donskikh.incidenthub.incident.IncidentSeverity;
import ru.donskikh.incidenthub.incident.IncidentPriority;

public record ConfirmAlertGroupRequest(
        @NotNull @PositiveOrZero Long expectedVersion,
        @NotNull IncidentSeverity severity,
        @NotNull IncidentPriority priority,
        @Positive Long businessServiceId,
        @Size(min = 1, max = 255) String title,
        @Size(min = 1) String description
) {}
