package ru.donskikh.incidenthub.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@ConfigurationProperties("app.security.integration")
public final class IntegrationTokenProperties {
    static final String LOCAL_TOKEN = "incident-hub-local-integration-token";
    private final byte[] tokenDigest;

    public IntegrationTokenProperties(String token, boolean allowLocalToken) {
        if (token == null || token.isBlank()) {
            throw new IllegalStateException("INTEGRATION_TOKEN must be configured outside the local profile");
        }
        if (LOCAL_TOKEN.equals(token) && !allowLocalToken) {
            throw new IllegalStateException("The repository integration token may only be used with the local profile");
        }
        this.tokenDigest = digest(token);
    }

    public boolean matches(String candidate) {
        return MessageDigest.isEqual(tokenDigest, digest(candidate));
    }

    private static byte[] digest(String value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    @Override
    public String toString() {
        return "IntegrationTokenProperties[token=REDACTED]";
    }
}
