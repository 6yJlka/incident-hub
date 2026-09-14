package ru.donskikh.incidenthub.identity.application;

import ru.donskikh.incidenthub.identity.UserRole;

public record CreateUserResult(
        Long userId,
        String email,
        String displayName,
        UserRole role,
        boolean active
) {
}
