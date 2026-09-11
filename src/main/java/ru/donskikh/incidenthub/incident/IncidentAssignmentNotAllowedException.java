package ru.donskikh.incidenthub.incident;

import ru.donskikh.incidenthub.common.DomainConflictException;

import java.util.List;

public class IncidentAssignmentNotAllowedException extends DomainConflictException {

    public IncidentAssignmentNotAllowedException(Long incidentId, IncidentStatus status) {
        super(createMessage(incidentId, status), status, List.of(IncidentStatus.OPEN, IncidentStatus.ASSIGNED));
    }

    private static String createMessage(Long incidentId, IncidentStatus status) {
        String incidentReference = incidentId == null ? "Incident" : "Incident " + incidentId;
        return incidentReference + " cannot be assigned in status " + status;
    }
}
