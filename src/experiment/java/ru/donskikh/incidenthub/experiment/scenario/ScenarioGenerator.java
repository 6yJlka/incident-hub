package ru.donskikh.incidenthub.experiment.scenario;

import ru.donskikh.incidenthub.experiment.graph.ServiceGraph;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;

public final class ScenarioGenerator {
    public ScenarioSample generate(ScenarioGenerationCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        var parameters = command.parameters();
        var graph = command.trueGraph();
        var seeds = ScenarioSeeds.from(command.seed());
        var failureRandom = new Random(seeds.failures());
        var noiseRandom = new Random(seeds.noise());
        var signalRandom = new Random(seeds.signals());
        var detectionRandom = new Random(seeds.detection());
        var serviceIds = graph.services().stream().map(service -> service.id()).toList();
        if (!serviceIds.containsAll(parameters.detection().overrides().keySet())
                || !serviceIds.containsAll(parameters.detection().probabilityOverrides().keySet())) {
            throw new IllegalArgumentException("Detection override references a service outside the true graph");
        }
        var failures = failures(graph, parameters, failureRandom);
        var alerts = new ArrayList<ScenarioAlert>();
        var labels = new TreeMap<String, TruthLabel>();
        var hidden = new TreeSet<String>();
        for (Failure failure : failures) {
            var times = propagate(graph, failure, parameters, failureRandom);
            for (var entry : times.entrySet()) {
                long detection = parameters.detection().overrides()
                        .getOrDefault(entry.getKey(), parameters.detection().defaultDistribution()).sampleMillis(failureRandom);
                var distribution = entry.getKey() == failure.rootServiceId()
                        ? parameters.signals().root() : parameters.signals().propagated();
                var signal = distribution.sample(signalRandom);
                String key = key(failureRandom, labels);
                alerts.add(new ScenarioAlert(key, entry.getKey(), parameters.start().plusMillis(Math.addExact(entry.getValue(), detection)),
                        signal.type(), signal.severity()));
                labels.put(key, new TruthLabel.FailureReference(failure.id()));
                double probability = parameters.detection().probabilityOverrides()
                        .getOrDefault(entry.getKey(), parameters.detection().detectionProbability());
                // Draw for every activation, including probabilities 0 and 1, to keep filtering monotone.
                if (detectionRandom.nextDouble() >= probability) hidden.add(key);
            }
        }
        double rate = parameters.noise().ratePerMinute();
        if (rate > 0) {
            double time = 0;
            double period = parameters.injectionPeriod().toMillis();
            while (true) {
                double uniform;
                do { uniform = noiseRandom.nextDouble(); } while (uniform == 0);
                double next = time + -StrictMath.log(uniform) * 60_000.0 / rate;
                if (next >= period) break;
                if (!(next > time)) throw new IllegalArgumentException("Noise intensity exceeds floating-point time resolution");
                time = next;
                long serviceId = serviceIds.get(noiseRandom.nextInt(serviceIds.size()));
                var signal = parameters.signals().noise().sample(signalRandom);
                String key = key(noiseRandom, labels);
                alerts.add(new ScenarioAlert(key, serviceId, parameters.start().plusMillis((long) time), signal.type(), signal.severity()));
                labels.put(key, new TruthLabel.Noise());
            }
        }
        // Reserve hidden keys until noise generation finishes, preserving collision handling as well.
        alerts.removeIf(alert -> hidden.contains(alert.externalKey()));
        hidden.forEach(labels::remove);
        return new ScenarioSample(alerts, new GroundTruth(labels, failures));
    }

    private List<Failure> failures(ServiceGraph graph, ScenarioParameters parameters, Random random) {
        var services = graph.services().stream().map(service -> service.id()).toList();
        List<Failure> result;
        if (parameters.failures() instanceof FailurePlan.Explicit explicit) {
            result = explicit.failures();
        } else {
            var selected = (FailurePlan.Selected) parameters.failures();
            if (selected.count() > services.size()) throw new IllegalArgumentException("Not enough distinct root services");
            var candidates = new ArrayList<>(services);
            if (selected.mode() == FailurePlan.RootSelectionMode.PATH_CONNECTED_TO_FIRST) {
                candidates.removeIf(root -> related(graph, root).size() < selected.count() - 1);
                if (candidates.isEmpty()) throw new IllegalArgumentException("No root has enough path-related services");
            }
            long first = candidates.get(random.nextInt(candidates.size()));
            var remaining = selected.mode() == FailurePlan.RootSelectionMode.PATH_CONNECTED_TO_FIRST
                    ? new ArrayList<>(related(graph, first)) : new ArrayList<>(services);
            remaining.remove(first);
            Collections.shuffle(remaining, random);
            var roots = new ArrayList<Long>();
            roots.add(first);
            roots.addAll(remaining.subList(0, selected.count() - 1));
            result = new ArrayList<>();
            for (int index = 0; index < roots.size(); index++) {
                Duration offset = selected.firstStartOffset().plus(selected.spacing().multipliedBy(index));
                result.add(new Failure("F" + (index + 1), roots.get(index), parameters.start().plus(offset)));
            }
        }
        for (Failure failure : result) {
            if (!services.contains(failure.rootServiceId()) || failure.startedAt().isBefore(parameters.start())
                    || !failure.startedAt().isBefore(parameters.start().plus(parameters.injectionPeriod()))
                    || failure.startedAt().getNano() % 1_000_000 != 0) {
                throw new IllegalArgumentException("Failure must reference the true graph and start within the millisecond-aligned injection period");
            }
        }
        return result.stream().sorted(Comparator.comparing(Failure::startedAt).thenComparing(Failure::id)).toList();
    }

    private TreeSet<Long> related(ServiceGraph graph, long root) {
        var result = reachable(graph, root, false);
        result.addAll(reachable(graph, root, true));
        result.remove(root);
        return result;
    }

    private TreeSet<Long> reachable(ServiceGraph graph, long root, boolean reverse) {
        var seen = new TreeSet<Long>();
        var queue = new ArrayDeque<Long>();
        queue.add(root);
        while (!queue.isEmpty()) {
            long current = queue.removeFirst();
            if (!seen.add(current)) continue;
            for (var edge : graph.edges()) {
                if ((reverse ? edge.dependencyId() : edge.dependentId()) == current) {
                    queue.add(reverse ? edge.dependentId() : edge.dependencyId());
                }
            }
        }
        return seen;
    }

    private SortedMap<Long, Long> propagate(ServiceGraph graph, Failure failure, ScenarioParameters parameters, Random random) {
        var outgoing = new TreeMap<Long, List<Transmission>>();
        for (var edge : graph.edges()) {
            double draw = random.nextDouble();
            long delay = parameters.propagationDelay().sampleMillis(random);
            if (draw < parameters.propagationProbability()) {
                outgoing.computeIfAbsent(edge.dependencyId(), ignored -> new ArrayList<>())
                        .add(new Transmission(edge.dependentId(), delay));
            }
        }
        var times = new TreeMap<Long, Long>();
        var queue = new PriorityQueue<>(Comparator.comparingLong(Arrival::time).thenComparingLong(Arrival::serviceId));
        long rootTime = Duration.between(parameters.start(), failure.startedAt()).toMillis();
        times.put(failure.rootServiceId(), rootTime);
        queue.add(new Arrival(failure.rootServiceId(), rootTime));
        while (!queue.isEmpty()) {
            var current = queue.remove();
            if (current.time() != times.get(current.serviceId())) continue;
            for (Transmission edge : outgoing.getOrDefault(current.serviceId(), List.of())) {
                long next = Math.addExact(current.time(), edge.delay());
                Long previous = times.get(edge.serviceId());
                if (previous == null || next < previous) {
                    times.put(edge.serviceId(), next);
                    queue.add(new Arrival(edge.serviceId(), next));
                }
            }
        }
        return times;
    }

    private String key(Random random, SortedMap<String, TruthLabel> used) {
        String value;
        do { value = HexFormat.of().toHexDigits(random.nextLong()) + HexFormat.of().toHexDigits(random.nextLong()); }
        while (used.containsKey(value));
        return value;
    }

    private record Transmission(long serviceId, long delay) {}
    private record Arrival(long serviceId, long time) {}
}
