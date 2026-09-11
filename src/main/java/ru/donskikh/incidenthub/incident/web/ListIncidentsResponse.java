package ru.donskikh.incidenthub.incident.web;

import java.util.List;

public record ListIncidentsResponse(
        List<IncidentListItemResponse> items,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext,
        boolean hasPrevious
) {
}
