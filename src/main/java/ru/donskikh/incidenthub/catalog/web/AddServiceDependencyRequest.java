package ru.donskikh.incidenthub.catalog.web;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import ru.donskikh.incidenthub.catalog.DependencyType;

public record AddServiceDependencyRequest(
        @Schema(description = "Identifier of the service required by the service in the path", example = "41")
        @NotNull @Positive Long dependencyServiceId,
        @Schema(description = "Interaction type: synchronous call, asynchronous messaging, or data dependency",
                example = "SYNC", allowableValues = {"SYNC", "ASYNC", "DATA"})
        @NotNull DependencyType type
) {
}
