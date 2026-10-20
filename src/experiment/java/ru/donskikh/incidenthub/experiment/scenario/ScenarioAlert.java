package ru.donskikh.incidenthub.experiment.scenario;

import ru.donskikh.incidenthub.alert.AlertSeverity;
import ru.donskikh.incidenthub.alert.AlertSignalType;

import java.time.Instant;
import java.util.Comparator;
import java.util.Objects;

public record ScenarioAlert(String externalKey, long serviceId, Instant occurredAt,
        AlertSignalType signalType, AlertSeverity severity) {
    public static final Comparator<ScenarioAlert> ORDER = Comparator.comparing(ScenarioAlert::occurredAt)
            .thenComparing(ScenarioAlert::externalKey);

    public ScenarioAlert {
        if (externalKey == null || externalKey.isBlank() || serviceId <= 0) {
            throw new IllegalArgumentException("Alert needs a nonblank external key and positive service id");
        }
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        Objects.requireNonNull(signalType, "signalType must not be null");
        Objects.requireNonNull(severity, "severity must not be null");
    }
}
