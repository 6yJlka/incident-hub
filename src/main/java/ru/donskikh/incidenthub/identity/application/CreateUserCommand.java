package ru.donskikh.incidenthub.identity.application;

import ru.donskikh.incidenthub.identity.UserRole;

import java.util.Objects;

public record CreateUserCommand(String email, String displayName, String password, UserRole role) {

    public CreateUserCommand {
        requireText(email, "email");
        requireText(displayName, "displayName");
        requireText(password, "password");
        Objects.requireNonNull(role, "role must not be null");
    }

    private static void requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
    }
}
