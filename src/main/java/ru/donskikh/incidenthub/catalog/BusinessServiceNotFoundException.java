package ru.donskikh.incidenthub.catalog;

import ru.donskikh.incidenthub.common.DomainNotFoundException;

public class BusinessServiceNotFoundException extends DomainNotFoundException {

    public BusinessServiceNotFoundException(long businessServiceId) {
        super("Business service not found: " + businessServiceId);
    }
}
