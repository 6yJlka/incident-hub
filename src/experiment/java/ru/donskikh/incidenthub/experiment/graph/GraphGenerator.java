package ru.donskikh.incidenthub.experiment.graph;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.TreeSet;

public final class GraphGenerator {
    private final ServiceGraph demoGraph;

    public GraphGenerator() { this(null); }

    public GraphGenerator(ServiceGraph demoGraph) { this.demoGraph = demoGraph; }

    public ServiceGraph generate(GraphGenerationCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        if (command.spec() instanceof GraphSpec.Demo) {
            return Objects.requireNonNull(demoGraph, "Demo graph must be supplied by the resource adapter");
        }
        var random = new Random(command.seed());
        List<Pair> pairs = switch (command.spec()) {
            case GraphSpec.Tree tree -> tree(tree);
            case GraphSpec.Layered layered -> layered(layered, random);
            case GraphSpec.Diamonds diamonds -> diamonds(diamonds);
            case GraphSpec.Cyclic cyclic -> cyclic(cyclic, random);
            case GraphSpec.Demo ignored -> throw new IllegalStateException("Demo graph was handled above");
        };
        int count = pairs.stream().mapToInt(pair -> Math.max(pair.from(), pair.to())).max().orElse(0) + 1;
        var ids = new ArrayList<Long>();
        var services = new ArrayList<ServiceNode>();
        for (int index = 1; index <= count; index++) {
            ids.add((long) index);
            services.add(new ServiceNode(index, "SERVICE_" + index, "Service " + index, command.tier()));
        }
        Collections.shuffle(ids, random);
        var edges = pairs.stream().map(pair -> new DependencyEdge(ids.get(pair.from()), ids.get(pair.to()), command.edgeType())).toList();
        return new ServiceGraph(services, edges);
    }

    private List<Pair> tree(GraphSpec.Tree spec) {
        var edges = new ArrayList<Pair>();
        int first = 0;
        int width = 1;
        int next = 1;
        for (int level = 0; level < spec.depth(); level++) {
            int children = Math.multiplyExact(width, spec.branching());
            int end = Math.addExact(next, children);
            for (int child = next; child < end; child++) edges.add(new Pair(child, first + (child - next) / spec.branching()));
            first = next;
            next = end;
            width = children;
        }
        return edges;
    }

    private List<Pair> diamonds(GraphSpec.Diamonds spec) {
        var edges = new ArrayList<Pair>();
        Math.addExact(1, Math.multiplyExact(spec.count(), Math.subtractExact(Math.multiplyExact(2, spec.branchDepth()), 1)));
        int anchor = 0;
        int next = 1;
        for (int index = 0; index < spec.count(); index++) {
            int left = anchor;
            int right = anchor;
            for (int depth = 1; depth < spec.branchDepth(); depth++) {
                edges.add(new Pair(next, left));
                left = next++;
                edges.add(new Pair(next, right));
                right = next++;
            }
            edges.add(new Pair(next, left));
            edges.add(new Pair(next, right));
            anchor = next++;
        }
        return edges;
    }

    private List<Pair> layered(GraphSpec.Layered spec, Random random) {
        if (spec.layers() == 1) return List.of();
        Math.multiplyExact(spec.layers(), spec.width());
        var selected = new TreeSet<Pair>();
        int width = spec.width();
        int previousAnchor = random.nextInt(width);
        int currentAnchor = width + random.nextInt(width);
        for (int index = 0; index < width; index++) {
            selected.add(new Pair(currentAnchor, index));
            selected.add(new Pair(width + index, previousAnchor));
        }
        for (int layer = 2; layer < spec.layers(); layer++) {
            for (int index = 0; index < width; index++) {
                selected.add(new Pair(layer * width + index, (layer - 1) * width + random.nextInt(width)));
            }
        }
        var remaining = new ArrayList<Pair>();
        for (int layer = 1; layer < spec.layers(); layer++) {
            for (int from = 0; from < width; from++) {
                for (int to = 0; to < width; to++) {
                    var edge = new Pair(layer * width + from, (layer - 1) * width + to);
                    if (!selected.contains(edge)) remaining.add(edge);
                }
            }
        }
        Collections.shuffle(remaining, random);
        long possible = Math.multiplyExact(Math.multiplyExact((long) spec.layers() - 1, width), width);
        int required = Math.toIntExact(Math.round(spec.density() * possible));
        selected.addAll(remaining.subList(0, required - selected.size()));
        return List.copyOf(selected);
    }

    private List<Pair> cyclic(GraphSpec.Cyclic spec, Random random) {
        var forward = new ArrayList<>(layered(spec.base(), random));
        var candidates = new ArrayList<>(forward);
        Collections.shuffle(candidates, random);
        int count = Math.max(1, Math.toIntExact(Math.round(spec.reverseEdgeRate() * forward.size())));
        for (Pair edge : candidates.subList(0, count)) forward.add(new Pair(edge.to(), edge.from()));
        return forward;
    }

    private record Pair(int from, int to) implements Comparable<Pair> {
        @Override
        public int compareTo(Pair other) {
            int first = Integer.compare(from, other.from);
            return first == 0 ? Integer.compare(to, other.to) : first;
        }
    }
}
