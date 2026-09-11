package ru.donskikh.incidenthub.catalog.web;

import java.util.List;

public record ListBusinessServicesResponse(
        List<BusinessServiceListItemResponse> items,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext,
        boolean hasPrevious
) {
}
