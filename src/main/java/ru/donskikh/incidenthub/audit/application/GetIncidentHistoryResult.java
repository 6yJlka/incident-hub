package ru.donskikh.incidenthub.audit.application;

import java.util.List;
import java.util.Objects;

public record GetIncidentHistoryResult(
        long incidentId,
        List<IncidentHistoryItem> items
) {

    public GetIncidentHistoryResult {
        Objects.requireNonNull(items, "items must not be null");
        items = List.copyOf(items);
    }
}
