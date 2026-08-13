package ru.donskikh.incidenthub.incident.application;

public record ReopenIncidentCommand(long incidentId) {

    public ReopenIncidentCommand {
        if (incidentId <= 0) {
            throw new IllegalArgumentException("incidentId must be positive");
        }
    }
}
