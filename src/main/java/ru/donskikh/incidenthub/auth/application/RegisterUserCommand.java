package ru.donskikh.incidenthub.auth.application;

public record RegisterUserCommand(String email, String displayName, String password) {

    public RegisterUserCommand {
        requireText(email, "email");
        requireText(displayName, "displayName");
        requireText(password, "password");
    }

    private static void requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
    }
}
