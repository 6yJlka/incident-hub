package ru.donskikh.incidenthub.incident;

import ru.donskikh.incidenthub.common.DomainNotFoundException;

public class IncidentNotFoundException extends DomainNotFoundException {

    public IncidentNotFoundException(long incidentId) {
        super("Incident not found: " + incidentId);
    }
}
