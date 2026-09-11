package ru.donskikh.incidenthub.incident.web;

import ru.donskikh.incidenthub.incident.IncidentStatus;

public record CreateIncidentResponse(
        Long incidentId,
        IncidentStatus status
) {
}
