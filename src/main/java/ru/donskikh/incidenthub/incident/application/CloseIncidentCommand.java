package ru.donskikh.incidenthub.incident.application;

public record CloseIncidentCommand(long incidentId) {

    public CloseIncidentCommand {
        if (incidentId <= 0) {
            throw new IllegalArgumentException("incidentId must be positive");
        }
    }
}
