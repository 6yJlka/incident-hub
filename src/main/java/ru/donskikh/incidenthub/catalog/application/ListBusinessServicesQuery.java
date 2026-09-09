package ru.donskikh.incidenthub.catalog.application;

import ru.donskikh.incidenthub.catalog.ServiceTier;

public record ListBusinessServicesQuery(
        int page,
        int size,
        Long ownerTeamId,
        ServiceTier tier,
        Boolean active
) {

    private static final int MAX_PAGE_SIZE = 100;

    public ListBusinessServicesQuery {
        if (page < 0) {
            throw new IllegalArgumentException("page must be greater than or equal to 0");
        }

        if (size <= 0) {
            throw new IllegalArgumentException("size must be greater than 0");
        }

        if (size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("size must not exceed " + MAX_PAGE_SIZE);
        }

        if (ownerTeamId != null && ownerTeamId <= 0) {
            throw new IllegalArgumentException("ownerTeamId must be positive");
        }
    }
}
