package ru.donskikh.incidenthub.incident.application;

import ru.donskikh.incidenthub.incident.IncidentPriority;
import ru.donskikh.incidenthub.incident.IncidentSeverity;

public record CreateIncidentCommand(
        String title,
        String description,
        long affectedServiceId,
        IncidentPriority priority,
        IncidentSeverity severity,
        long actorId,
        Long responsibleTeamId
) {

    public CreateIncidentCommand {
        if (affectedServiceId <= 0) {
            throw new IllegalArgumentException("affectedServiceId must be positive");
        }

        if (actorId <= 0) {
            throw new IllegalArgumentException("actorId must be positive");
        }

        if (responsibleTeamId != null && responsibleTeamId <= 0) {
            throw new IllegalArgumentException("responsibleTeamId must be positive");
        }
    }
}
