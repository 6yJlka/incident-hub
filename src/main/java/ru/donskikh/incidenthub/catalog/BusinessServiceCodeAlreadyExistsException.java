package ru.donskikh.incidenthub.catalog;

public class BusinessServiceCodeAlreadyExistsException extends RuntimeException {

    public BusinessServiceCodeAlreadyExistsException(String code) {
        super("Business service code already exists: " + code);
    }

    public BusinessServiceCodeAlreadyExistsException(String code, Throwable cause) {
        super("Business service code already exists: " + code, cause);
    }
}
