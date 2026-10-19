package ru.donskikh.incidenthub.catalog.web;

import io.swagger.v3.oas.annotations.media.Schema;
import ru.donskikh.incidenthub.catalog.DependencyType;

public record AffectedDependencyResponse(
        @Schema(description = "Direct dependency within the root and affected service set") Long serviceId,
        DependencyType type
) {
}
