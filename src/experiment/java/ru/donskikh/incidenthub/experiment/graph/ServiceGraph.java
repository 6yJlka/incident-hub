package ru.donskikh.incidenthub.experiment.graph;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.TreeSet;

public record ServiceGraph(List<ServiceNode> services, List<DependencyEdge> edges) {
    public ServiceGraph {
        services = Objects.requireNonNull(services, "services must not be null").stream()
                .sorted(Comparator.comparingLong(ServiceNode::id)).toList();
        edges = Objects.requireNonNull(edges, "edges must not be null").stream().sorted(DependencyEdge.ORDER).toList();
        if (services.isEmpty()) throw new IllegalArgumentException("Graph must contain a service");
        var ids = new TreeSet<Long>();
        var codes = new TreeSet<String>();
        for (ServiceNode service : services) {
            if (!ids.add(service.id()) || !codes.add(service.code())) {
                throw new IllegalArgumentException("Service ids and codes must be unique");
            }
        }
        DependencyEdge previous = null;
        for (DependencyEdge edge : edges) {
            if (!ids.contains(edge.dependentId()) || !ids.contains(edge.dependencyId())) {
                throw new IllegalArgumentException("Edge references a service outside the graph");
            }
            if (previous != null && DependencyEdge.ORDER.compare(previous, edge) == 0) {
                throw new IllegalArgumentException("Duplicate directed edge");
            }
            previous = edge;
        }
    }
}
