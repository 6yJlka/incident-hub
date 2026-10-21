package ru.donskikh.incidenthub.alert.correlation;

import java.util.List;
import java.util.Objects;

public record CorrelationContext(List<OpenAlertGroupSnapshot> groups, DependencyGraphSnapshot graph) {
    public CorrelationContext {
        Objects.requireNonNull(graph, "graph must not be null");
        groups = List.copyOf(Objects.requireNonNull(groups, "groups must not be null"));
    }
    public CorrelationContext(List<OpenAlertGroupSnapshot> groups) { this(groups, DependencyGraphSnapshot.empty()); }
}
