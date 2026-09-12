package ru.donskikh.incidenthub.team.application;

public record CreateTeamCommand(String code, String name) {

    public CreateTeamCommand {
        requireText(code, "code");
        requireText(name, "name");
    }

    private static void requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
    }
}
