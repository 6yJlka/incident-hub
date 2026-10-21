package ru.donskikh.incidenthub.experiment.scenario;

import java.time.Instant;
import java.util.Objects;

public record Failure(String id, long rootServiceId, Instant startedAt) {
    public Failure {
        if (id == null || id.isBlank() || rootServiceId <= 0) {
            throw new IllegalArgumentException("Failure needs a nonblank id and positive root service id");
        }
        Objects.requireNonNull(startedAt, "startedAt must not be null");
    }
}
