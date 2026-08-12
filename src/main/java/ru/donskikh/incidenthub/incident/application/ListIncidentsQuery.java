package ru.donskikh.incidenthub.incident.application;

import ru.donskikh.incidenthub.incident.IncidentCategory;
import ru.donskikh.incidenthub.incident.IncidentPriority;
import ru.donskikh.incidenthub.incident.IncidentSource;
import ru.donskikh.incidenthub.incident.IncidentStatus;

public record ListIncidentsQuery(
        int page,
        int size,
        IncidentStatus status,
        IncidentPriority priority,
        IncidentCategory category,
        IncidentSource source,
        Long responsibleTeamId
) {

    private static final int MAX_PAGE_SIZE = 100;

    public ListIncidentsQuery {
        if (page < 0) {
            throw new IllegalArgumentException("page must be greater than or equal to 0");
        }

        if (size <= 0) {
            throw new IllegalArgumentException("size must be greater than 0");
        }

        if (size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("size must not exceed " + MAX_PAGE_SIZE);
        }

        if (responsibleTeamId != null && responsibleTeamId <= 0) {
            throw new IllegalArgumentException("responsibleTeamId must be positive");
        }
    }
}
