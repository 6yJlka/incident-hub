package ru.donskikh.incidenthub.catalog;

import ru.donskikh.incidenthub.common.DomainConflictException;

public class BusinessServiceCodeAlreadyExistsException extends DomainConflictException {

    public BusinessServiceCodeAlreadyExistsException(String code) {
        super("Business service code already exists: " + code);
    }

    public BusinessServiceCodeAlreadyExistsException(String code, Throwable cause) {
        super("Business service code already exists: " + code, cause);
    }
}
