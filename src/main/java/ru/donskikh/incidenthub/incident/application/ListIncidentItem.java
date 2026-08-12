package ru.donskikh.incidenthub.incident.application;

import ru.donskikh.incidenthub.incident.IncidentCategory;
import ru.donskikh.incidenthub.incident.IncidentPriority;
import ru.donskikh.incidenthub.incident.IncidentSource;
import ru.donskikh.incidenthub.incident.IncidentStatus;

import java.time.Instant;

public record ListIncidentItem(
        Long id,
        String title,
        IncidentCategory category,
        IncidentSource source,
        IncidentPriority priority,
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
