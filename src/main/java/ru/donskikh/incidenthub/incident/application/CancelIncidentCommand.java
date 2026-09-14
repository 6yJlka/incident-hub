package ru.donskikh.incidenthub.incident.application;

public record CancelIncidentCommand(long incidentId, long actorId) {

    public CancelIncidentCommand {
        if (incidentId <= 0) {
            throw new IllegalArgumentException("incidentId must be positive");
        }
        if (actorId <= 0) {
            throw new IllegalArgumentException("actorId must be positive");
        }
    }
}
