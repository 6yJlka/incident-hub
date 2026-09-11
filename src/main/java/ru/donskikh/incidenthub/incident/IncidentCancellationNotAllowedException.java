package ru.donskikh.incidenthub.incident;

import ru.donskikh.incidenthub.common.DomainConflictException;

import java.util.List;

public class IncidentCancellationNotAllowedException extends DomainConflictException {

    private final IncidentStatus status;

    public IncidentCancellationNotAllowedException(Long incidentId, IncidentStatus status) {
        super(
                createMessage(incidentId, status),
                status,
                List.of(IncidentStatus.OPEN, IncidentStatus.ASSIGNED, IncidentStatus.IN_PROGRESS)
        );
        this.status = status;
    }

    public IncidentStatus getStatus() {
        return status;
    }

    private static String createMessage(Long incidentId, IncidentStatus status) {
        String incidentReference = incidentId == null ? "Incident" : "Incident " + incidentId;
        return incidentReference + " cannot be cancelled in status " + status;
    }
}
