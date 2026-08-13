package ru.donskikh.incidenthub.incident.application;

import ru.donskikh.incidenthub.incident.IncidentStatus;

public record ResolveIncidentResult(
        Long incidentId,
        IncidentStatus status
) {
}
