package ru.donskikh.incidenthub.identity.web;

public record CreateUserResponse(Long userId, String email, boolean active) {
}
