package ru.donskikh.incidenthub.alertgroup.application;

import java.util.List;
import java.util.Objects;
import java.time.Instant;

public record GetAlertGroupResult(AlertGroupItem group, List<GroupAlertItem> alerts,
        Long decidedById, String decidedByDisplayName, Instant decidedAt, String dismissReason, long version) {
    public GetAlertGroupResult {
        Objects.requireNonNull(group, "group must not be null");
        alerts = List.copyOf(Objects.requireNonNull(alerts, "alerts must not be null"));
    }

    public GetAlertGroupResult(AlertGroupItem group, List<GroupAlertItem> alerts) {
        this(group, alerts, null, null, null, null, 0);
    }
}
