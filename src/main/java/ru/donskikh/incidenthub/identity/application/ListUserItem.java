package ru.donskikh.incidenthub.identity.application;

import java.time.Instant;

public record ListUserItem(
        Long id,
        String email,
        String displayName,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
}
