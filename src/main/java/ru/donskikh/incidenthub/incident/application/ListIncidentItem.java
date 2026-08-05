package ru.donskikh.incidenthub.incident.application;

import ru.donskikh.incidenthub.incident.IncidentPriority;
import ru.donskikh.incidenthub.incident.IncidentStatus;

import java.time.Instant;

public record ListIncidentItem(
        Long id,
        String title,
        String category,
        IncidentPriority priority,
        IncidentStatus status,
        Long reporterId,
        String reporterDisplayName,
        Long assigneeId,
        String assigneeDisplayName,
        Instant createdAt,
        Instant updatedAt
) {
}
