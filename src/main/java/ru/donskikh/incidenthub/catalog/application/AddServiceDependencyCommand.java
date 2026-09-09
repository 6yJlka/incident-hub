package ru.donskikh.incidenthub.catalog.application;

import ru.donskikh.incidenthub.catalog.DependencyType;

import java.util.Objects;

public record AddServiceDependencyCommand(
        long dependentServiceId,
        long dependencyServiceId,
        DependencyType type
) {

    public AddServiceDependencyCommand {
        if (dependentServiceId <= 0) {
            throw new IllegalArgumentException("dependentServiceId must be positive");
        }

        if (dependencyServiceId <= 0) {
            throw new IllegalArgumentException("dependencyServiceId must be positive");
        }

        Objects.requireNonNull(type, "type must not be null");
    }
}
