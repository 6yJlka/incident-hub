package ru.donskikh.incidenthub.identity.application;

public record CreateUserCommand(String email, String displayName) {

    public CreateUserCommand {
        requireText(email, "email");
        requireText(displayName, "displayName");
    }

    private static void requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
    }
}
