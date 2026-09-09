package ru.donskikh.incidenthub.catalog.application;

import ru.donskikh.incidenthub.catalog.ServiceTier;

import java.util.Objects;

public record CreateBusinessServiceCommand(
        String code,
        String name,
        String description,
        long ownerTeamId,
        ServiceTier tier
) {

    public CreateBusinessServiceCommand {
        requireText(code, "code");
        requireText(name, "name");
        requireText(description, "description");

        if (ownerTeamId <= 0) {
            throw new IllegalArgumentException("ownerTeamId must be positive");
        }

        Objects.requireNonNull(tier, "tier must not be null");
    }

    private static void requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
    }
}
