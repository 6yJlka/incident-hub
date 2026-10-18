package ru.donskikh.incidenthub.alert.correlation;

import ru.donskikh.incidenthub.alert.AlertSeverity;
import ru.donskikh.incidenthub.alert.AlertSignalType;

import java.time.Instant;
import java.util.Objects;

public record CorrelationGroupMember(
        long businessServiceId,
        Instant occurredAt,
        AlertSignalType signalType,
        AlertSeverity severity
) {
    public CorrelationGroupMember {
        if (businessServiceId <= 0) {
            throw new IllegalArgumentException("businessServiceId must be positive");
        }
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        Objects.requireNonNull(signalType, "signalType must not be null");
        Objects.requireNonNull(severity, "severity must not be null");
    }
}
