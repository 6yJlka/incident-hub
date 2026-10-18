package ru.donskikh.incidenthub.alert.correlation;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record OpenAlertGroupSnapshot(
        long id,
        long probableRootServiceId,
        Instant firstAlertAt,
        Instant lastAlertAt,
        List<CorrelationGroupMember> members
) {
    public OpenAlertGroupSnapshot {
        if (id <= 0 || probableRootServiceId <= 0) {
            throw new IllegalArgumentException("Group and root service identifiers must be positive");
        }
        Objects.requireNonNull(firstAlertAt, "firstAlertAt must not be null");
        Objects.requireNonNull(lastAlertAt, "lastAlertAt must not be null");
        if (firstAlertAt.isAfter(lastAlertAt)) {
            throw new IllegalArgumentException("firstAlertAt must not be after lastAlertAt");
        }
        members = List.copyOf(Objects.requireNonNull(members, "members must not be null"));
    }
}
