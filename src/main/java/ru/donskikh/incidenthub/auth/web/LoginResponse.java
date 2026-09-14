package ru.donskikh.incidenthub.auth.web;

import io.swagger.v3.oas.annotations.media.Schema;

public record LoginResponse(
        @Schema(description = "Signed JWT access token") String accessToken,
        @Schema(example = "Bearer") String tokenType,
        @Schema(description = "Token lifetime in seconds", example = "3600") long expiresInSeconds
) {
}
