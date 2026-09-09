package ru.donskikh.incidenthub.catalog.application;

public record GetAffectedServicesQuery(
        long businessServiceId,
        int maxDepth
) {

    public static final int MAX_DEPTH = 10;

    public GetAffectedServicesQuery {
        if (businessServiceId <= 0) {
            throw new IllegalArgumentException("businessServiceId must be positive");
        }

        if (maxDepth <= 0) {
            throw new IllegalArgumentException("maxDepth must be greater than 0");
        }

        if (maxDepth > MAX_DEPTH) {
            throw new IllegalArgumentException("maxDepth must not exceed " + MAX_DEPTH);
        }
    }
}
