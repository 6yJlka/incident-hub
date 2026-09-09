package ru.donskikh.incidenthub.catalog.application;

import ru.donskikh.incidenthub.catalog.ServiceTier;

public record CreateBusinessServiceResult(
        Long businessServiceId,
        String code,
        ServiceTier tier,
        boolean active
) {
}
