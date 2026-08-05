package ru.donskikh.incidenthub.incident.application;

import ru.donskikh.incidenthub.incident.IncidentPriority;
import ru.donskikh.incidenthub.incident.IncidentStatus;

public record ListIncidentsQuery(
        int page,
        int size,
        IncidentStatus status,
        IncidentPriority priority,
        String category
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

        if (category != null) {
            category = category.trim();

            if (category.isEmpty()) {
                category = null;
            }
        }
    }
}
