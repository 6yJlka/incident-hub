package ru.donskikh.incidenthub.incident.application;

public record AssignIncidentCommand(
        long incidentId,
        long assigneeId,
        long actorId
) {

    public AssignIncidentCommand {
        if (incidentId <= 0) {
            throw new IllegalArgumentException("incidentId must be positive");
        }

        if (assigneeId <= 0) {
            throw new IllegalArgumentException("assigneeId must be positive");
        }

        if (actorId <= 0) {
            throw new IllegalArgumentException("actorId must be positive");
        }
    }
}
