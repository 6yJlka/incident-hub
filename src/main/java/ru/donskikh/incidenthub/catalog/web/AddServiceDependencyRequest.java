package ru.donskikh.incidenthub.catalog.web;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import ru.donskikh.incidenthub.catalog.DependencyType;

public record AddServiceDependencyRequest(
        @NotNull @Positive Long dependencyServiceId,
        @NotNull DependencyType type
) {
}
