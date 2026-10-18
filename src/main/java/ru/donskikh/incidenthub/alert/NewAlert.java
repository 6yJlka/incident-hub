package ru.donskikh.incidenthub.alert;

import java.time.Instant;
import java.util.Objects;

public record NewAlert(
        AlertKey key,
        long businessServiceId,
        AlertSignalType signalType,
        AlertSeverity severity,
        Instant occurredAt,
        Instant receivedAt,
        String description
) {
    public NewAlert {
        Objects.requireNonNull(key, "key must not be null");
        Objects.requireNonNull(signalType, "signalType must not be null");
        Objects.requireNonNull(severity, "severity must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        Objects.requireNonNull(receivedAt, "receivedAt must not be null");
        if (businessServiceId <= 0) {
            throw new IllegalArgumentException("businessServiceId must be positive");
        }
        if (description == null || description.isBlank() || description.length() > 2000) {
            throw new IllegalArgumentException("description must contain 1 to 2000 characters");
        }
    }
}
