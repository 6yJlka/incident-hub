package ru.donskikh.incidenthub.incident.application;

public record ResolveIncidentCommand(long incidentId, long actorId) {

    public ResolveIncidentCommand {
        if (incidentId <= 0) {
            throw new IllegalArgumentException("incidentId must be positive");
        }
        if (actorId <= 0) {
            throw new IllegalArgumentException("actorId must be positive");
        }
    }
}
