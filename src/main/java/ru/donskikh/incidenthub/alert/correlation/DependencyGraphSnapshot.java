package ru.donskikh.incidenthub.alert.correlation;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.TreeSet;

public record DependencyGraphSnapshot(List<Long> serviceIds, List<DependencyArc> edges) {
    public DependencyGraphSnapshot {
        serviceIds = Objects.requireNonNull(serviceIds, "serviceIds must not be null").stream().sorted().toList();
        if (serviceIds.stream().anyMatch(id -> id <= 0) || new TreeSet<>(serviceIds).size() != serviceIds.size()) {
            throw new IllegalArgumentException("Service ids must be unique and positive");
        }
        edges = Objects.requireNonNull(edges, "edges must not be null").stream().sorted(
                Comparator.comparingLong(DependencyArc::dependentServiceId).thenComparingLong(DependencyArc::dependencyServiceId)).toList();
        var pairs = new TreeSet<String>();
        for (var edge : edges) {
            if (!serviceIds.contains(edge.dependentServiceId()) || !serviceIds.contains(edge.dependencyServiceId())
                    || !pairs.add(edge.dependentServiceId() + ":" + edge.dependencyServiceId())) {
                throw new IllegalArgumentException("Unknown service or duplicate dependency pair");
            }
        }
    }
    public static DependencyGraphSnapshot empty() { return new DependencyGraphSnapshot(List.of(), List.of()); }
}
