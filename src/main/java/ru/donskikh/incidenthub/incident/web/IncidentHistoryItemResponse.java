package ru.donskikh.incidenthub.incident.web;

import ru.donskikh.incidenthub.audit.IncidentAuditEventType;
import ru.donskikh.incidenthub.incident.IncidentStatus;

import java.time.Instant;

public record IncidentHistoryItemResponse(
        Long id,
        IncidentAuditEventType eventType,
        IncidentStatus fromStatus,
        IncidentStatus toStatus,
        Instant createdAt
) {
}
