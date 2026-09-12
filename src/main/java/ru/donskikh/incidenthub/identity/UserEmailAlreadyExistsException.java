package ru.donskikh.incidenthub.identity;

import ru.donskikh.incidenthub.common.DomainConflictException;

public class UserEmailAlreadyExistsException extends DomainConflictException {

    public UserEmailAlreadyExistsException(String email) {
        super("User email already exists: " + email);
    }

    public UserEmailAlreadyExistsException(String email, Throwable cause) {
        super("User email already exists: " + email, cause);
    }
}
