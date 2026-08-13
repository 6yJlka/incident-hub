package ru.donskikh.incidenthub.incident.application;

import ru.donskikh.incidenthub.incident.IncidentStatus;

public record ReopenIncidentResult(
        Long incidentId,
        IncidentStatus status
) {
}
