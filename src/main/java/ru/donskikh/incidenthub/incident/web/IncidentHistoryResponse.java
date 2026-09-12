package ru.donskikh.incidenthub.incident.web;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record IncidentHistoryResponse(
        @Schema(description = "Incident identifier", example = "73") long incidentId,
        @Schema(description = "Chronological lifecycle events", example = "[]")
        List<IncidentHistoryItemResponse> items
) {
}
