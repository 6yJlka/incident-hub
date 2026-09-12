package ru.donskikh.incidenthub.team.application;

import java.time.Instant;

public record ListTeamItem(
        Long id,
        String code,
        String name,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
}
