package ru.donskikh.incidenthub.incident.web;

import io.swagger.v3.oas.annotations.media.Schema;

import ru.donskikh.incidenthub.audit.IncidentAuditEventType;
import ru.donskikh.incidenthub.incident.IncidentStatus;

import java.time.Instant;

public record IncidentHistoryItemResponse(
        @Schema(description = "Audit event identifier", example = "301") Long id,
        @Schema(description = "Lifecycle action that produced the event", example = "STARTED")
        IncidentAuditEventType eventType,
        @Schema(description = "Status before the action; null for CREATED", example = "ASSIGNED", nullable = true)
        IncidentStatus fromStatus,
        @Schema(description = "Status after the action", example = "IN_PROGRESS") IncidentStatus toStatus,
        @Schema(description = "Event timestamp", example = "2026-09-01T10:15:00Z") Instant createdAt
) {
}
