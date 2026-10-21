package ru.donskikh.incidenthub.experiment;

import ru.donskikh.incidenthub.catalog.DependencyType;
import ru.donskikh.incidenthub.catalog.ServiceTier;
import ru.donskikh.incidenthub.experiment.graph.DependencyEdge;
import ru.donskikh.incidenthub.experiment.graph.GraphGenerationCommand;
import ru.donskikh.incidenthub.experiment.graph.GraphSpec;
import ru.donskikh.incidenthub.experiment.graph.ServiceGraph;
import ru.donskikh.incidenthub.experiment.graph.ServiceNode;
import ru.donskikh.incidenthub.experiment.scenario.DelayDistribution;
import ru.donskikh.incidenthub.experiment.scenario.DetectionModel;
import ru.donskikh.incidenthub.experiment.scenario.Failure;
import ru.donskikh.incidenthub.experiment.scenario.FailurePlan;
import ru.donskikh.incidenthub.experiment.scenario.NoiseModel;
import ru.donskikh.incidenthub.experiment.scenario.ScenarioParameters;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.stream.IntStream;

public final class ExperimentFixtures {
    private ExperimentFixtures() {}

    public static DependencyEdge edge(long from, long to) {
        return new DependencyEdge(from, to, DependencyType.SYNC);
    }

    public static ServiceGraph graph(int count, DependencyEdge... edges) {
        return new ServiceGraph(IntStream.rangeClosed(1, count)
                .mapToObj(id -> new ServiceNode(id, "S" + id, "Service " + id, ServiceTier.TIER_2)).toList(), List.of(edges));
    }

    public static GraphGenerationCommand graphCommand(GraphSpec spec, long seed) {
        return new GraphGenerationCommand(spec, seed, ServiceTier.TIER_2, DependencyType.SYNC);
    }

    public static ScenarioParameters parameters() {
        var defaults = ExperimentDefaults.parameters().scenario();
        return new ScenarioParameters(defaults.start(), defaults.injectionPeriod(),
                new FailurePlan.Explicit(List.of(new Failure("F1", 1, defaults.start()))), 1,
                new DelayDistribution.Fixed(Duration.ZERO),
                new DetectionModel(new DelayDistribution.Fixed(Duration.ZERO), Map.of()), new NoiseModel(0), defaults.signals());
    }

    public static TreeSet<Long> reachable(ServiceGraph graph, long root, boolean reverse, boolean undirected) {
        var seen = new TreeSet<Long>();
        var queue = new ArrayDeque<Long>();
        queue.add(root);
        while (!queue.isEmpty()) {
            long current = queue.removeFirst();
            if (!seen.add(current)) continue;
            graph.edges().forEach(edge -> {
                if ((reverse ? edge.dependencyId() : edge.dependentId()) == current) {
                    queue.add(reverse ? edge.dependentId() : edge.dependencyId());
                }
                if (undirected && (reverse ? edge.dependentId() : edge.dependencyId()) == current) {
                    queue.add(reverse ? edge.dependencyId() : edge.dependentId());
                }
            });
        }
        return seen;
    }

    public static boolean acyclic(ServiceGraph graph) {
        var remaining = new TreeSet<Long>();
        graph.services().forEach(service -> remaining.add(service.id()));
        while (!remaining.isEmpty()) {
            var sinks = remaining.stream().filter(id -> graph.edges().stream()
                    .noneMatch(edge -> edge.dependentId() == id && remaining.contains(edge.dependencyId()))).toList();
            if (sinks.isEmpty()) return false;
            remaining.removeAll(sinks);
        }
        return true;
    }
}
