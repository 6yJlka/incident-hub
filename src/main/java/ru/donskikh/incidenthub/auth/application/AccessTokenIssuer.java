package ru.donskikh.incidenthub.auth.application;

import ru.donskikh.incidenthub.identity.User;

public interface AccessTokenIssuer {

    IssuedAccessToken issue(User user);

    record IssuedAccessToken(String value, long expiresInSeconds) {
    }
}
