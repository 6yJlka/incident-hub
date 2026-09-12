package ru.donskikh.incidenthub.catalog.web;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record GetAffectedServicesResponse(
        @Schema(description = "Deduplicated affected services ordered by minimum depth", example = "[]")
        List<AffectedServiceResponse> items,
        @Schema(description = "Identifier of the failed root service", example = "41") long businessServiceId,
        @Schema(description = "Maximum dependency depth traversed by the query", example = "3") int maxDepth
) {
}
