package ru.donskikh.incidenthub.identity.web;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(
        @Schema(description = "Unique email; surrounding spaces are removed and case is normalized", example = "engineer@example.com")
        @NotBlank @Email @Size(max = 255) String email,
        @Schema(description = "Name shown in incident views", example = "Elena Sokolova")
        @NotBlank @Size(max = 150) String displayName
) {

    public CreateUserRequest {
        if (email != null) {
            email = email.trim();
        }
    }
}
