package ru.donskikh.incidenthub.alert.correlation;

import java.util.List;
import java.util.Objects;

public record CorrelationContext(List<OpenAlertGroupSnapshot> groups) {
    public CorrelationContext {
        groups = List.copyOf(Objects.requireNonNull(groups, "groups must not be null"));
    }
}
