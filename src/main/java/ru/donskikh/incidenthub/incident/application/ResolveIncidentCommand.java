package ru.donskikh.incidenthub.incident.application;

public record ResolveIncidentCommand(long incidentId) {

    public ResolveIncidentCommand {
        if (incidentId <= 0) {
            throw new IllegalArgumentException("incidentId must be positive");
        }
    }
}
