package ru.donskikh.incidenthub.catalog.application;

public record RemoveServiceDependencyCommand(
        long dependentServiceId,
        long dependencyServiceId
) {

    public RemoveServiceDependencyCommand {
        if (dependentServiceId <= 0) {
            throw new IllegalArgumentException("dependentServiceId must be positive");
        }

        if (dependencyServiceId <= 0) {
            throw new IllegalArgumentException("dependencyServiceId must be positive");
        }
    }
}
