package ru.donskikh.incidenthub.incident.application;

import ru.donskikh.incidenthub.incident.IncidentStatus;

public record CloseIncidentResult(
        Long incidentId,
        IncidentStatus status
) {
}
