package ru.donskikh.incidenthub.team;

import ru.donskikh.incidenthub.common.DomainConflictException;

public class TeamCodeAlreadyExistsException extends DomainConflictException {

    public TeamCodeAlreadyExistsException(String code) {
        super("Team code already exists: " + code);
    }

    public TeamCodeAlreadyExistsException(String code, Throwable cause) {
        super("Team code already exists: " + code, cause);
    }
}
