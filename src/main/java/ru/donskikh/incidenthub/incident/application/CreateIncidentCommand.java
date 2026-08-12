package ru.donskikh.incidenthub.incident.application;

import ru.donskikh.incidenthub.incident.IncidentCategory;
import ru.donskikh.incidenthub.incident.IncidentPriority;

public record CreateIncidentCommand(
        String title,
        String description,
        IncidentCategory category,
        IncidentPriority priority,
        long reporterId,
        Long responsibleTeamId
) {

    public CreateIncidentCommand {
        if (reporterId <= 0) {
            throw new IllegalArgumentException("reporterId must be positive");
        }

        if (responsibleTeamId != null && responsibleTeamId <= 0) {
            throw new IllegalArgumentException("responsibleTeamId must be positive");
        }
    }
}
