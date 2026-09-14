package ru.donskikh.incidenthub.auth.application;

import ru.donskikh.incidenthub.identity.UserRole;

public record RegisterUserResult(
        Long userId,
        String email,
        String displayName,
        UserRole role,
        boolean active
) {
}
