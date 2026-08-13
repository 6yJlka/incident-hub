package ru.donskikh.incidenthub.incident;

public enum IncidentStatus {
    OPEN,
    ASSIGNED,
    IN_PROGRESS,
    RESOLVED,
    CLOSED,
    CANCELLED;

    public boolean allowsAssignment() {
        return this == OPEN || this == ASSIGNED;
    }

    public boolean allowsStartProgress() {
        return this == ASSIGNED;
    }
}
