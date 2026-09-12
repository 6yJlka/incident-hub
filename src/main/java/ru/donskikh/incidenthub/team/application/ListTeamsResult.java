package ru.donskikh.incidenthub.team.application;

import java.util.List;
import java.util.Objects;

public record ListTeamsResult(
        List<ListTeamItem> items,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext,
        boolean hasPrevious
) {

    public ListTeamsResult {
        Objects.requireNonNull(items, "items must not be null");
        items = List.copyOf(items);
    }
}
