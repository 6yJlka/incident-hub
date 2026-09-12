package ru.donskikh.incidenthub.identity.web;

import java.util.List;

public record ListUsersResponse(
        List<UserListItemResponse> items,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext,
        boolean hasPrevious
) {
}
