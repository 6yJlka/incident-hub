package ru.donskikh.incidenthub.identity.application;

import ru.donskikh.incidenthub.identity.UserRole;

public record GetCurrentUserResult(
        Long id,
        String email,
        String displayName,
        UserRole role,
        boolean active
) {
}
