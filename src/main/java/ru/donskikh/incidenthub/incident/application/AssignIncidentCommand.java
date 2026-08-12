package ru.donskikh.incidenthub.incident.application;

public record AssignIncidentCommand(
        long incidentId,
        long assigneeId
) {

    public AssignIncidentCommand {
        if (incidentId <= 0) {
            throw new IllegalArgumentException("incidentId must be positive");
        }

        if (assigneeId <= 0) {
            throw new IllegalArgumentException("assigneeId must be positive");
        }
    }
}
