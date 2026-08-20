package ru.donskikh.incidenthub.audit.application;

import ru.donskikh.incidenthub.audit.IncidentAuditEventType;
import ru.donskikh.incidenthub.incident.IncidentStatus;

import java.time.Instant;

public record IncidentHistoryItem(
        Long id,
        IncidentAuditEventType eventType,
        IncidentStatus fromStatus,
        IncidentStatus toStatus,
        Instant createdAt
) {
}
