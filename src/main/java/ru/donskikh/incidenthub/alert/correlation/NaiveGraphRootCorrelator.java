package ru.donskikh.incidenthub.alert.correlation;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.TreeMap;

public final class NaiveGraphRootCorrelator implements AlertCorrelator {
    private final TimeWindowCorrelator grouping;
    private final NaiveGraphRootParameters parameters;

    public NaiveGraphRootCorrelator(Duration window, NaiveGraphRootParameters parameters) {
        this.grouping = new TimeWindowCorrelator(window);
        this.parameters = Objects.requireNonNull(parameters, "parameters must not be null");
    }
    @Override
    public CorrelationDecision correlate(CorrelationAlert alert, CorrelationContext context) {
        Objects.requireNonNull(alert, "alert must not be null");
        Objects.requireNonNull(context, "context must not be null");
        var decision = grouping.correlate(alert, context);
        var members = new ArrayList<CorrelationGroupMember>();
        if (decision instanceof CorrelationDecision.JoinGroup join) {
            members.addAll(context.groups().stream().filter(group -> group.id() == join.groupId()).findFirst().orElseThrow().members());
        }
        members.add(new CorrelationGroupMember(alert.id(), alert.businessServiceId(), alert.occurredAt(), alert.signalType(), alert.severity()));
        var assessment = rank(members, context.graph());
        return decision instanceof CorrelationDecision.JoinGroup join
                ? new CorrelationDecision.JoinGroup(join.groupId(), assessment, join.linkStrength(), join.linkEvidence())
                : new CorrelationDecision.OpenGroup(assessment, decision.linkEvidence());
    }
    private RootAssessment.Ranked rank(List<CorrelationGroupMember> members, DependencyGraphSnapshot graph) {
        var weights = new TreeMap<Long, Double>();
        // Sort alerts so floating-point sums do not depend on snapshot iteration order.
        members.stream().sorted(Comparator.comparingLong(CorrelationGroupMember::alertId)).forEach(member ->
                weights.merge(member.businessServiceId(), parameters.severityWeights().get(member.severity()), Double::sum));
        double total = weights.values().stream().mapToDouble(Double::doubleValue).sum();
        if (!Double.isFinite(total)) throw new IllegalArgumentException("Severity weight sum overflow");
        var ids = new ArrayList<>(weights.keySet());
        var positions = new TreeMap<Long, Integer>();
        for (int i = 0; i < ids.size(); i++) positions.put(ids.get(i), i);
        var outgoing = new ArrayList<List<Integer>>();
        for (int i = 0; i < ids.size(); i++) outgoing.add(new ArrayList<>());
        for (var edge : graph.edges()) {
            if (positions.containsKey(edge.dependentServiceId()) && positions.containsKey(edge.dependencyServiceId())) {
                outgoing.get(positions.get(edge.dependentServiceId())).add(positions.get(edge.dependencyServiceId()));
            }
        }
        double[] teleport = new double[ids.size()];
        for (int i = 0; i < ids.size(); i++) teleport[i] = weights.get(ids.get(i)) / total;
        double[] ranks = teleport.clone();
        for (int iteration = 1; iteration <= parameters.maxIterations(); iteration++) {
            double dangling = 0;
            for (int i = 0; i < ids.size(); i++) if (outgoing.get(i).isEmpty()) dangling += ranks[i];
            double[] next = new double[ids.size()];
            for (int i = 0; i < ids.size(); i++) next[i] = (1 - parameters.damping() + parameters.damping() * dangling) * teleport[i];
            for (int i = 0; i < ids.size(); i++) {
                for (int target : outgoing.get(i)) next[target] += parameters.damping() * ranks[i] / outgoing.get(i).size();
            }
            double residual = 0;
            for (int i = 0; i < ids.size(); i++) residual += Math.abs(next[i] - ranks[i]);
            ranks = next;
            if (residual <= parameters.tolerance()) {
                var candidates = new ArrayList<RootCandidate>();
                for (int i = 0; i < ids.size(); i++) candidates.add(new RootCandidate(ids.get(i), ranks[i]));
                candidates.sort(Comparator.comparingDouble(RootCandidate::score).reversed().thenComparingLong(RootCandidate::serviceId));
                var evidence = new ArrayList<Evidence>();
                for (var candidate : candidates) evidence.add(new Evidence.PageRankScore(candidate.serviceId(), candidate.score(),
                        weights.get(candidate.serviceId()), parameters.damping(), iteration, residual));
                return new RootAssessment.Ranked(candidates, evidence);
            }
        }
        throw new IllegalStateException("PageRank did not converge within maxIterations");
    }
}
