package ru.donskikh.incidenthub.incident;

import ru.donskikh.incidenthub.common.DomainConflictException;

public class IncidentResolutionNotAllowedException extends DomainConflictException {

    private final IncidentStatus status;

    public IncidentResolutionNotAllowedException(Long incidentId, IncidentStatus status) {
        super(createMessage(incidentId, status), status, IncidentStatus.IN_PROGRESS);
        this.status = status;
    }

    public IncidentStatus getStatus() {
        return status;
    }

    private static String createMessage(Long incidentId, IncidentStatus status) {
        String incidentReference = incidentId == null ? "Incident" : "Incident " + incidentId;
        return incidentReference + " cannot be resolved in status " + status;
    }
}
