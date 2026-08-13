package ru.donskikh.incidenthub.incident;

public class IncidentReopenNotAllowedException extends RuntimeException {

    private final IncidentStatus status;

    public IncidentReopenNotAllowedException(Long incidentId, IncidentStatus status) {
        super(createMessage(incidentId, status));
        this.status = status;
    }

    public IncidentStatus getStatus() {
        return status;
    }

    private static String createMessage(Long incidentId, IncidentStatus status) {
        String incidentReference = incidentId == null ? "Incident" : "Incident " + incidentId;
        return incidentReference + " cannot be reopened in status " + status;
    }
}
