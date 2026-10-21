package ru.donskikh.incidenthub.experiment.graph;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Objects;
import java.util.Random;
import java.util.TreeSet;

public final class GraphDistorter {
    public ServiceGraph distort(DistortGraphCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        var graph = command.trueGraph();
        var parameters = command.distortion();
        int removedCount = Math.toIntExact(Math.round(parameters.missingEdgeRate() * graph.edges().size()));
        int addedCount = Math.toIntExact(Math.round(parameters.spuriousEdgeRate() * graph.edges().size()));
        long complementSize = Math.multiplyExact((long) graph.services().size(), graph.services().size() - 1L) - graph.edges().size();
        if (addedCount > complementSize) throw new IllegalArgumentException("Spurious edge count exceeds the true graph complement");
        if (removedCount == 0 && addedCount == 0) return graph;
        var random = new Random(parameters.seed());
        var shuffled = new ArrayList<>(graph.edges());
        Collections.shuffle(shuffled, random);
        var edges = new ArrayList<>(shuffled.subList(removedCount, shuffled.size()));
        if (addedCount != 0) {
            var existing = new TreeSet<>(DependencyEdge.ORDER);
            existing.addAll(graph.edges());
            var complement = new ArrayList<DependencyEdge>();
            for (ServiceNode from : graph.services()) {
                for (ServiceNode to : graph.services()) {
                    if (from.id() != to.id()) {
                        var edge = new DependencyEdge(from.id(), to.id(), command.spuriousEdgeType());
                        if (!existing.contains(edge)) complement.add(edge);
                    }
                }
            }
            Collections.shuffle(complement, random);
            edges.addAll(complement.subList(0, addedCount));
        }
        return new ServiceGraph(graph.services(), edges);
    }
}
