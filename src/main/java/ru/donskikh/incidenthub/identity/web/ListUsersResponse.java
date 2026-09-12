package ru.donskikh.incidenthub.identity.web;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record ListUsersResponse(
        @Schema(description = "Users on the requested page", example = "[]") List<UserListItemResponse> items,
        @Schema(description = "Zero-based page index", example = "0") int page,
        @Schema(description = "Actual page size", example = "20") int size,
        @Schema(description = "Total number of matching users", example = "6") long totalElements,
        @Schema(description = "Total number of pages", example = "1") int totalPages,
        @Schema(description = "Whether another page follows", example = "false") boolean hasNext,
        @Schema(description = "Whether a previous page exists", example = "false") boolean hasPrevious
) {
}
