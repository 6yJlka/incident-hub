package ru.donskikh.incidenthub.catalog.web;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record ListBusinessServicesResponse(
        @Schema(description = "Services on the requested page", example = "[]") List<BusinessServiceListItemResponse> items,
        @Schema(description = "Zero-based page index", example = "0") int page,
        @Schema(description = "Actual page size", example = "20") int size,
        @Schema(description = "Total number of matching services", example = "10") long totalElements,
        @Schema(description = "Total number of pages", example = "1") int totalPages,
        @Schema(description = "Whether another page follows", example = "false") boolean hasNext,
        @Schema(description = "Whether a previous page exists", example = "false") boolean hasPrevious
) {
}
