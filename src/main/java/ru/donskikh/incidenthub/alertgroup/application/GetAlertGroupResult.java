package ru.donskikh.incidenthub.alertgroup.application;

import java.util.List;
import java.util.Objects;

public record GetAlertGroupResult(AlertGroupItem group, List<GroupAlertItem> alerts) {
    public GetAlertGroupResult {
        Objects.requireNonNull(group, "group must not be null");
        alerts = List.copyOf(Objects.requireNonNull(alerts, "alerts must not be null"));
    }
}
