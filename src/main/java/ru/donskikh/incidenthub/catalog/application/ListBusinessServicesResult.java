package ru.donskikh.incidenthub.catalog.application;

import java.util.List;
import java.util.Objects;

public record ListBusinessServicesResult(
        List<ListBusinessServiceItem> items,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext,
        boolean hasPrevious
) {

    public ListBusinessServicesResult {
        Objects.requireNonNull(items, "items must not be null");
        items = List.copyOf(items);
    }
}
