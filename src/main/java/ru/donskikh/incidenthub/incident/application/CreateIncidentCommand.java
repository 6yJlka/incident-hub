package ru.donskikh.incidenthub.incident.application;

import ru.donskikh.incidenthub.incident.IncidentPriority;

public record CreateIncidentCommand(
        String title,
        String description,
        String category,
        IncidentPriority priority,
        long reporterId
) {

    public CreateIncidentCommand {
        if (reporterId <= 0) {
            throw new IllegalArgumentException("reporterId must be positive");
        }
    }
}
