package ru.donskikh.incidenthub.incident;

public class IncidentStartProgressNotAllowedException extends RuntimeException {

    private final IncidentStatus status;

    public IncidentStartProgressNotAllowedException(Long incidentId, IncidentStatus status) {
        super(createMessage(incidentId, status));
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
