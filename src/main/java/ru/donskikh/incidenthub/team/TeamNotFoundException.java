package ru.donskikh.incidenthub.team;

public class TeamNotFoundException extends RuntimeException {

    public TeamNotFoundException(long teamId) {
        super("Team not found: " + teamId);
    }
}
