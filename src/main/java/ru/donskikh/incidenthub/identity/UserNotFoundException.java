package ru.donskikh.incidenthub.identity;

import ru.donskikh.incidenthub.common.DomainNotFoundException;

public class UserNotFoundException extends DomainNotFoundException {

    public UserNotFoundException(long userId) {
        super("User not found: " + userId);
    }
}
