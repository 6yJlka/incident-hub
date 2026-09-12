package ru.donskikh.incidenthub.team.web;

import java.time.Instant;

public record TeamListItemResponse(
        Long id,
        String code,
        String name,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
}
