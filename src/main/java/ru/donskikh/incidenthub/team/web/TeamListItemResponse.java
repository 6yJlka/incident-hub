package ru.donskikh.incidenthub.team.web;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

public record TeamListItemResponse(
        @Schema(description = "Team identifier", example = "12") Long id,
        @Schema(description = "Normalized team code", example = "PLATFORM") String code,
        @Schema(description = "Human-readable team name", example = "Platform Engineering") String name,
        @Schema(description = "Whether the team is active", example = "true") boolean active,
        @Schema(description = "Creation timestamp", example = "2026-09-01T10:00:00Z") Instant createdAt,
        @Schema(description = "Last update timestamp", example = "2026-09-02T11:00:00Z") Instant updatedAt
) {
}
