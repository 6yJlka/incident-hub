package ru.donskikh.incidenthub.alert.application;

import ru.donskikh.incidenthub.alert.AlertKey;
import ru.donskikh.incidenthub.alert.AlertSignalType;
import ru.donskikh.incidenthub.alert.AlertSeverity;

import java.time.Instant;
import java.util.Locale;
import java.util.Objects;

public record IncomingAlert(
        String source,
        String externalId,
        String serviceCode,
        AlertSignalType signalType,
        AlertSeverity severity,
        Instant occurredAt,
        String description
) {
    public IncomingAlert {
        new AlertKey(source, externalId);
        Objects.requireNonNull(signalType, "signalType must not be null");
        Objects.requireNonNull(severity, "severity must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        if (serviceCode == null || serviceCode.isBlank() || serviceCode.length() > 50) {
            throw new IllegalArgumentException("serviceCode must contain 1 to 50 characters");
        }
        serviceCode = serviceCode.trim().toUpperCase(Locale.ROOT);
        if (description == null || description.isBlank() || description.length() > 2000) {
            throw new IllegalArgumentException("description must contain 1 to 2000 characters");
        }
    }

    public AlertKey key() {
        return new AlertKey(source, externalId);
    }
}
