package ru.donskikh.incidenthub.common;

public abstract class DomainNotFoundException extends RuntimeException {

    protected DomainNotFoundException(String message) {
        super(message);
    }

    protected DomainNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
