package ru.donskikh.incidenthub.incident;

import java.util.Set;

public enum IncidentStatus {
    OPEN,
    ASSIGNED,
    IN_PROGRESS,
    RESOLVED,
    CLOSED,
    CANCELLED;

    private static final Set<IncidentStatus> ACTIVE_STATUSES = Set.of(OPEN, ASSIGNED, IN_PROGRESS);

    public static Set<IncidentStatus> activeStatuses() {
        return ACTIVE_STATUSES;
    }

    public boolean allowsAssignment() {
        return this == OPEN || this == ASSIGNED;
    }

    public boolean allowsStartProgress() {
        return this == ASSIGNED;
    }

    public boolean allowsResolve() {
        return this == IN_PROGRESS;
    }

    public boolean allowsClose() {
        return this == RESOLVED;
    }

    public boolean allowsReopen() {
        return this == RESOLVED;
    }

    public boolean allowsCancellation() {
        return this == OPEN || this == ASSIGNED || this == IN_PROGRESS;
    }
}
