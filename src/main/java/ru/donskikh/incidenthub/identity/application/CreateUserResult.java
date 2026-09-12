package ru.donskikh.incidenthub.identity.application;

public record CreateUserResult(Long userId, String email, boolean active) {
}
