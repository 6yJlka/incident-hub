package ru.donskikh.incidenthub.catalog.application;

public record RemoveServiceDependencyResult(
        Long dependentServiceId,
        Long dependencyServiceId
) {
}
