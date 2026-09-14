package ru.donskikh.incidenthub.security;

import io.jsonwebtoken.io.Decoders;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties("app.security.jwt")
public record JwtProperties(String secret, Duration accessTokenTtl, boolean allowLocalSecret) {

    static final String LOCAL_DEVELOPMENT_SECRET =
            "aW5jaWRlbnQtaHViLWxvY2FsLWRldmVsb3BtZW50LXNlY3JldC0yMDI2";

    public JwtProperties {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("JWT_SECRET must be configured outside the local profile");
        }
        byte[] keyBytes;
        try {
            keyBytes = Decoders.BASE64.decode(secret);
        } catch (RuntimeException exception) {
            throw new IllegalStateException("JWT_SECRET must be a Base64-encoded value", exception);
        }
        if (keyBytes.length < 32) {
            throw new IllegalStateException("JWT_SECRET must contain at least 256 bits");
        }
        if (LOCAL_DEVELOPMENT_SECRET.equals(secret) && !allowLocalSecret) {
            throw new IllegalStateException("The repository JWT secret may only be used with the local profile");
        }
        if (accessTokenTtl == null || accessTokenTtl.isZero() || accessTokenTtl.isNegative()) {
            throw new IllegalStateException("JWT access token lifetime must be positive");
        }
    }
}
