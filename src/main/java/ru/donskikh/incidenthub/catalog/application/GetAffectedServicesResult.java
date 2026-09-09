package ru.donskikh.incidenthub.catalog.application;

import java.util.List;
import java.util.Objects;

public record GetAffectedServicesResult(
        List<AffectedServiceItem> items,
        long businessServiceId,
        int maxDepth
) {

    public GetAffectedServicesResult {
        Objects.requireNonNull(items, "items must not be null");
        items = List.copyOf(items);
    }
}
