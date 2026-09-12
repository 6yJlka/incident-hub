package ru.donskikh.incidenthub.identity.application;

import java.util.List;
import java.util.Objects;

public record ListUsersResult(
        List<ListUserItem> items,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext,
        boolean hasPrevious
) {

    public ListUsersResult {
        Objects.requireNonNull(items, "items must not be null");
        items = List.copyOf(items);
    }
}
