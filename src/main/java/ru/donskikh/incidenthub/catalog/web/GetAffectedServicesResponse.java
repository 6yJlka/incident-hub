package ru.donskikh.incidenthub.catalog.web;

import java.util.List;

public record GetAffectedServicesResponse(
        List<AffectedServiceResponse> items,
        long businessServiceId,
        int maxDepth
) {
}
