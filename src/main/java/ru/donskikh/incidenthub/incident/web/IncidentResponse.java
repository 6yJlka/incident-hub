package ru.donskikh.incidenthub.incident.web;

import io.swagger.v3.oas.annotations.media.Schema;

import ru.donskikh.incidenthub.incident.IncidentPriority;
import ru.donskikh.incidenthub.incident.IncidentSeverity;
import ru.donskikh.incidenthub.incident.IncidentSource;
import ru.donskikh.incidenthub.incident.IncidentStatus;

import java.time.Instant;

public record IncidentResponse(
        @Schema(description = "Incident identifier", example = "73") Long id,
        @Schema(description = "Incident title", example = "Card payment authorization failures") String title,
        @Schema(description = "Observed symptoms and operational context",
                example = "Authorization requests fail for one acquiring bank") String description,
        @Schema(description = "Affected service identifier", example = "42") Long affectedServiceId,
        @Schema(description = "Affected service code", example = "PAYMENT_API") String affectedServiceCode,
        @Schema(description = "Affected service name", example = "Payment API") String affectedServiceName,
        @Schema(description = "How the incident was registered", example = "MANUAL") IncidentSource source,
        @Schema(description = "Work ordering urgency", example = "CRITICAL") IncidentPriority priority,
        @Schema(description = "Business impact level, independent of priority", example = "SEV1")
        IncidentSeverity severity,
        @Schema(description = "Current lifecycle status", example = "IN_PROGRESS") IncidentStatus status,
        @Schema(description = "Reporter identifier", example = "20") Long reporterId,
        @Schema(description = "Reporter display name", example = "Anna Ivanova") String reporterDisplayName,
        @Schema(description = "Responsible team identifier", example = "12", nullable = true) Long responsibleTeamId,
        @Schema(description = "Responsible team name", example = "Payments Platform", nullable = true)
        String responsibleTeamName,
        @Schema(description = "Responsible team code", example = "PAYMENTS", nullable = true) String responsibleTeamCode,
        @Schema(description = "Assigned user identifier", example = "21", nullable = true) Long assigneeId,
        @Schema(description = "Assigned user display name", example = "Maksim Kuznetsov", nullable = true)
        String assigneeDisplayName,
        @Schema(description = "Creation timestamp", example = "2026-09-01T10:00:00Z") Instant createdAt,
        @Schema(description = "Last update timestamp", example = "2026-09-01T10:15:00Z") Instant updatedAt
) {
}
