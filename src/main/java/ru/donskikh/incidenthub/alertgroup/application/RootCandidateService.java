package ru.donskikh.incidenthub.alertgroup.application;

import java.util.Objects;

public record RootCandidateService(long serviceId, String code, String name) {
    public RootCandidateService {
        if (serviceId <= 0) throw new IllegalArgumentException("serviceId must be positive");
        Objects.requireNonNull(code, "code must not be null");
        Objects.requireNonNull(name, "name must not be null");
    }
}
