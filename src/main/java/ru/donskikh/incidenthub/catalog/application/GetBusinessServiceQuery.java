package ru.donskikh.incidenthub.catalog.application;

public record GetBusinessServiceQuery(long businessServiceId) {

    public GetBusinessServiceQuery {
        if (businessServiceId <= 0) {
            throw new IllegalArgumentException("businessServiceId must be positive");
        }
    }
}
