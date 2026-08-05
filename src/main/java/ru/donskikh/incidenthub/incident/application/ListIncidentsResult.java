package ru.donskikh.incidenthub.incident.application;

import java.util.List;
import java.util.Objects;

public record ListIncidentsResult(
        List<ListIncidentItem> items,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext,
        boolean hasPrevious
) {

    public ListIncidentsResult {
        Objects.requireNonNull(items, "items must not be null");
        items = List.copyOf(items);
    }
}