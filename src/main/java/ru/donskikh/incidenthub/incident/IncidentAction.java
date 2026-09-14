package ru.donskikh.incidenthub.incident;

import java.util.Objects;
import java.util.function.Predicate;

public enum IncidentAction {
    ASSIGN(IncidentStatus::allowsAssignment),
    START(IncidentStatus::allowsStartProgress),
    RESOLVE(IncidentStatus::allowsResolve),
    CLOSE(IncidentStatus::allowsClose),
    REOPEN(IncidentStatus::allowsReopen),
    CANCEL(IncidentStatus::allowsCancellation);

    private final Predicate<IncidentStatus> statusRule;

    IncidentAction(Predicate<IncidentStatus> statusRule) {
        this.statusRule = statusRule;
    }

    public boolean isAllowedFrom(IncidentStatus status) {
        return statusRule.test(Objects.requireNonNull(status, "status must not be null"));
    }
}
