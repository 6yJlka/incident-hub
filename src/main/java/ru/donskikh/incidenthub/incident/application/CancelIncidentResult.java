package ru.donskikh.incidenthub.incident.application;

import ru.donskikh.incidenthub.incident.IncidentStatus;

public record CancelIncidentResult(
        Long incidentId,
        IncidentStatus status
) {
}
