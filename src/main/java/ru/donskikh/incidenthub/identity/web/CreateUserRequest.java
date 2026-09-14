package ru.donskikh.incidenthub.identity.web;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import ru.donskikh.incidenthub.identity.UserRole;

public record CreateUserRequest(
        @Schema(description = "Unique email; surrounding spaces are removed and case is normalized", example = "engineer@example.com")
        @NotBlank @Email @Size(max = 255) String email,
        @Schema(description = "Name shown in incident views", example = "Elena Sokolova")
        @NotBlank @Size(max = 150) String displayName,
        @Schema(description = "Initial password", example = "secure-password-123")
        @NotBlank @Size(min = 8, max = 72) String password,
        @Schema(description = "Role assigned by an administrator", example = "ENGINEER")
        @NotNull UserRole role
) {

    public CreateUserRequest {
        if (email != null) {
            email = email.trim();
        }
    }
}
