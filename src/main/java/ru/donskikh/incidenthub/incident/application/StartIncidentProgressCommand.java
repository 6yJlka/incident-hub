package ru.donskikh.incidenthub.incident.application;

public record StartIncidentProgressCommand(long incidentId, long actorId) {

    public StartIncidentProgressCommand {
        if (incidentId <= 0) {
            throw new IllegalArgumentException("incidentId must be positive");
        }
        if (actorId <= 0) {
            throw new IllegalArgumentException("actorId must be positive");
        }
    }
}
