package ru.donskikh.incidenthub.team;

import ru.donskikh.incidenthub.common.DomainNotFoundException;

public class TeamNotFoundException extends DomainNotFoundException {

    public TeamNotFoundException(long teamId) {
        super("Team not found: " + teamId);
    }
}
