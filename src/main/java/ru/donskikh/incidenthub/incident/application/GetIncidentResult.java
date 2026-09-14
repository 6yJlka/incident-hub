package ru.donskikh.incidenthub.incident.application;

import ru.donskikh.incidenthub.incident.IncidentPriority;
import ru.donskikh.incidenthub.incident.IncidentSeverity;
import ru.donskikh.incidenthub.incident.IncidentSource;
import ru.donskikh.incidenthub.incident.IncidentStatus;
import ru.donskikh.incidenthub.incident.IncidentAction;

import java.time.Instant;
import java.util.List;

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
        Instant updatedAt,
        List<IncidentAction> availableActions
) {
    public GetIncidentResult {
        availableActions = List.copyOf(availableActions);
    }

    public GetIncidentResult withAvailableActions(List<IncidentAction> actions) {
        return new GetIncidentResult(
                id,
                title,
                description,
                affectedServiceId,
                affectedServiceCode,
                affectedServiceName,
                source,
                priority,
                severity,
                status,
                reporterId,
                reporterDisplayName,
                responsibleTeamId,
                responsibleTeamName,
                responsibleTeamCode,
                assigneeId,
                assigneeDisplayName,
                createdAt,
                updatedAt,
                actions
        );
    }
}
