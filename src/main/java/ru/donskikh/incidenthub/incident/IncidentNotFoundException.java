package ru.donskikh.incidenthub.incident;

public class IncidentNotFoundException extends RuntimeException {

    public IncidentNotFoundException(long incidentId) {
        super("Incident not found: " + incidentId);
    }
}
