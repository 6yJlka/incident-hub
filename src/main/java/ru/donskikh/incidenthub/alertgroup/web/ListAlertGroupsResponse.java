package ru.donskikh.incidenthub.alertgroup.web;

import java.util.List;

public record ListAlertGroupsResponse(
        List<AlertGroupResponse> items,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext,
        boolean hasPrevious
) {
    public ListAlertGroupsResponse {
        items = List.copyOf(items);
    }
}
