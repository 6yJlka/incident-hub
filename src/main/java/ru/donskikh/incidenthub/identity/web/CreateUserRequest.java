package ru.donskikh.incidenthub.identity.web;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(
        @NotBlank @Email @Size(max = 255) String email,
        @NotBlank @Size(max = 150) String displayName
) {

    public CreateUserRequest {
        if (email != null) {
            email = email.trim();
        }
    }
}
