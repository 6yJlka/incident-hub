package ru.donskikh.incidenthub.auth.application;

public record LoginCommand(String email, String password) {

    public LoginCommand {
        requireText(email, "email");
        requireText(password, "password");
    }

    private static void requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
    }
}
