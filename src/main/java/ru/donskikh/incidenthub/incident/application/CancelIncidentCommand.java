package ru.donskikh.incidenthub.incident.application;

public record CancelIncidentCommand(long incidentId) {

    public CancelIncidentCommand {
        if (incidentId <= 0) {
            throw new IllegalArgumentException("incidentId must be positive");
        }
    }
}
