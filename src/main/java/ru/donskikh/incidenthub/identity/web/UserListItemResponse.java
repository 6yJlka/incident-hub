package ru.donskikh.incidenthub.identity.web;

import java.time.Instant;

public record UserListItemResponse(
        Long id,
        String email,
        String displayName,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
}
