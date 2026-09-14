package ru.donskikh.incidenthub.auth.web;

import io.swagger.v3.oas.annotations.media.Schema;
import ru.donskikh.incidenthub.identity.UserRole;

public record RegisterResponse(
        @Schema(example = "21") Long userId,
        @Schema(example = "reporter@example.com") String email,
        @Schema(example = "Example Reporter") String displayName,
        @Schema(example = "REPORTER") UserRole role,
        @Schema(example = "true") boolean active
) {
}
