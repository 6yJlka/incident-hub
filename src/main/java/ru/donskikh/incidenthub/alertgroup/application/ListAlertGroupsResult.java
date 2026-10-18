package ru.donskikh.incidenthub.alertgroup.application;

import java.util.List;
import java.util.Objects;

public record ListAlertGroupsResult(
        List<AlertGroupItem> items,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext,
        boolean hasPrevious
) {
    public ListAlertGroupsResult {
        items = List.copyOf(Objects.requireNonNull(items, "items must not be null"));
    }
}
