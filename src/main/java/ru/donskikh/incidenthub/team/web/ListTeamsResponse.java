package ru.donskikh.incidenthub.team.web;

import java.util.List;

public record ListTeamsResponse(
        List<TeamListItemResponse> items,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext,
        boolean hasPrevious
) {
}
