package ru.donskikh.incidenthub.auth.web;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank
        @Email
        @Size(max = 255)
        @Schema(example = "reporter@example.com")
        String email,

        @NotBlank
        @Size(max = 150)
        @Schema(example = "Example Reporter")
        String displayName,

        @NotBlank
        @Size(min = 8, max = 72)
        @Schema(example = "secure-password-123")
        String password
) {
}
