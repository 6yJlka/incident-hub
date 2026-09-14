package ru.donskikh.incidenthub.auth.application;

public record LoginResult(String accessToken, String tokenType, long expiresInSeconds) {
}
