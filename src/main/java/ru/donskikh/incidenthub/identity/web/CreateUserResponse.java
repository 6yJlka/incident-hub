package ru.donskikh.incidenthub.identity.web;

import io.swagger.v3.oas.annotations.media.Schema;
import ru.donskikh.incidenthub.identity.UserRole;

public record CreateUserResponse(
        @Schema(description = "Generated user identifier", example = "21") Long userId,
        @Schema(description = "Normalized email", example = "engineer@example.com") String email,
        @Schema(description = "Name shown in incident views", example = "Elena Sokolova") String displayName,
        @Schema(description = "Assigned role", example = "ENGINEER") UserRole role,
        @Schema(description = "Whether the user is active", example = "true") boolean active
) {
}
