package ru.donskikh.incidenthub.identity.web;

import io.swagger.v3.oas.annotations.media.Schema;

public record CreateUserResponse(
        @Schema(description = "Generated user identifier", example = "21") Long userId,
        @Schema(description = "Normalized email", example = "engineer@example.com") String email,
        @Schema(description = "Whether the user is active", example = "true") boolean active
) {
}
