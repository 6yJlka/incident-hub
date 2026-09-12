package ru.donskikh.incidenthub.identity.web;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

public record UserListItemResponse(
        @Schema(description = "User identifier", example = "21") Long id,
        @Schema(description = "Normalized user email", example = "engineer@example.com") String email,
        @Schema(description = "Display name", example = "Elena Sokolova") String displayName,
        @Schema(description = "Whether the user is active", example = "true") boolean active,
        @Schema(description = "Creation timestamp", example = "2026-09-01T10:00:00Z") Instant createdAt,
        @Schema(description = "Last update timestamp", example = "2026-09-02T11:00:00Z") Instant updatedAt
) {
}
