package ru.donskikh.incidenthub.incident;

public class IncidentAssignmentNotAllowedException extends RuntimeException {

    public IncidentAssignmentNotAllowedException(Long incidentId, IncidentStatus status) {
        super(createMessage(incidentId, status));
    }

    private static String createMessage(Long incidentId, IncidentStatus status) {
        String incidentReference = incidentId == null ? "Incident" : "Incident " + incidentId;
        return incidentReference + " cannot be assigned in status " + status;
    }
}
