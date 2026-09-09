package ru.donskikh.incidenthub.incident.application;

import ru.donskikh.incidenthub.incident.IncidentPriority;
import ru.donskikh.incidenthub.incident.IncidentSeverity;
import ru.donskikh.incidenthub.incident.IncidentSource;
import ru.donskikh.incidenthub.incident.IncidentStatus;

import java.time.Instant;

public record GetIncidentResult(
        Long id,
        String title,
        String description,
        Long affectedServiceId,
        String affectedServiceCode,
        String affectedServiceName,
        IncidentSource source,
        IncidentPriority priority,
        IncidentSeverity severity,
        IncidentStatus status,
        Long reporterId,
        String reporterDisplayName,
        Long responsibleTeamId,
        String responsibleTeamName,
        String responsibleTeamCode,
        Long assigneeId,
        String assigneeDisplayName,
        Instant createdAt,
        Instant updatedAt
) {
}
