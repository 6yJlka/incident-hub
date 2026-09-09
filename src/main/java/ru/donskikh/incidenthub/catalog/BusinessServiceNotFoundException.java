package ru.donskikh.incidenthub.catalog;

public class BusinessServiceNotFoundException extends RuntimeException {

    public BusinessServiceNotFoundException(long businessServiceId) {
        super("Business service not found: " + businessServiceId);
    }
}
