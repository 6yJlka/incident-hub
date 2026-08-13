package ru.donskikh.incidenthub.incident.application;

public record StartIncidentProgressCommand(long incidentId) {

    public StartIncidentProgressCommand {
        if (incidentId <= 0) {
            throw new IllegalArgumentException("incidentId must be positive");
        }
    }
}
