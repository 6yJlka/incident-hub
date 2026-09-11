package ru.donskikh.incidenthub.incident.web;

import java.util.List;

public record IncidentHistoryResponse(
        long incidentId,
        List<IncidentHistoryItemResponse> items
) {
}
