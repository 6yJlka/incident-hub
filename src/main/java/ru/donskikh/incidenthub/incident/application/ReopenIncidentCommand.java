package ru.donskikh.incidenthub.incident.application;

public record ReopenIncidentCommand(long incidentId, long actorId) {

    public ReopenIncidentCommand {
        if (incidentId <= 0) {
            throw new IllegalArgumentException("incidentId must be positive");
        }
        if (actorId <= 0) {
            throw new IllegalArgumentException("actorId must be positive");
        }
    }
}
