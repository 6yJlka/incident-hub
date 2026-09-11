package ru.donskikh.incidenthub.catalog.web;

import ru.donskikh.incidenthub.catalog.ServiceTier;

public record CreateBusinessServiceResponse(
        Long businessServiceId,
        String code,
        ServiceTier tier,
        boolean active
) {
}
