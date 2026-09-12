package ru.donskikh.incidenthub.incident;

import ru.donskikh.incidenthub.common.DomainConflictException;

public class IncidentStartProgressNotAllowedException extends DomainConflictException {

    private final IncidentStatus status;

    public IncidentStartProgressNotAllowedException(Long incidentId, IncidentStatus status) {
        super(createMessage(incidentId, status), status, IncidentStatus.ASSIGNED);
        this.status = status;
    }

    public IncidentStatus getStatus() {
        return status;
    }

    private static String createMessage(Long incidentId, IncidentStatus status) {
        String incidentReference = incidentId == null ? "Incident" : "Incident " + incidentId;
        return incidentReference + " cannot start progress in status " + status;
    }
}
