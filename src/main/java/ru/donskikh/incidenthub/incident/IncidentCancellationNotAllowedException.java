package ru.donskikh.incidenthub.incident;

public class IncidentCancellationNotAllowedException extends RuntimeException {

    private final IncidentStatus status;

    public IncidentCancellationNotAllowedException(Long incidentId, IncidentStatus status) {
        super(createMessage(incidentId, status));
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
