package ru.donskikh.incidenthub.incident.application;

import ru.donskikh.incidenthub.incident.IncidentStatus;

public record AssignIncidentResult(
        Long incidentId,
        Long assigneeId,
        IncidentStatus status
) {
}
