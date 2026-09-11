package ru.donskikh.incidenthub.incident;

import ru.donskikh.incidenthub.common.DomainConflictException;

public class IncidentClosureNotAllowedException extends DomainConflictException {

    private final IncidentStatus status;

    public IncidentClosureNotAllowedException(Long incidentId, IncidentStatus status) {
        super(createMessage(incidentId, status), status, IncidentStatus.RESOLVED);
        this.status = status;
    }

    public IncidentStatus getStatus() {
        return status;
    }

    private static String createMessage(Long incidentId, IncidentStatus status) {
        String incidentReference = incidentId == null ? "Incident" : "Incident " + incidentId;
        return incidentReference + " cannot be closed in status " + status;
    }
}
