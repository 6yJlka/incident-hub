package ru.donskikh.incidenthub.identity.application;

import ru.donskikh.incidenthub.identity.UserRole;

import java.time.Instant;

public record ListUserItem(
        Long id,
        String email,
        String displayName,
        UserRole role,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
}
