package ru.donskikh.incidenthub.security;

import ru.donskikh.incidenthub.identity.UserRole;

public record AuthenticatedUser(Long userId, String email, UserRole role) {
}
