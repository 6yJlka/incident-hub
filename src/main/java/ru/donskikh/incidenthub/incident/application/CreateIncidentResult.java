package ru.donskikh.incidenthub.incident.application;

import ru.donskikh.incidenthub.incident.IncidentStatus;

public record CreateIncidentResult(
        Long incidentId,
        IncidentStatus status
) {
}
