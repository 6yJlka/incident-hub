package ru.donskikh.incidenthub.experiment.metrics;

import ru.donskikh.incidenthub.alert.correlation.RootAssessment;
import ru.donskikh.incidenthub.experiment.replay.ReplayResult;
import ru.donskikh.incidenthub.experiment.scenario.GroundTruth;
import ru.donskikh.incidenthub.experiment.scenario.TruthLabel;
import java.util.Objects;
import java.util.TreeMap;
import java.util.TreeSet;

public final class ExperimentMetrics {
    public MetricsResult calculate(ReplayResult replay, GroundTruth truth) {
        Objects.requireNonNull(replay, "replay must not be null");
        Objects.requireNonNull(truth, "truth must not be null");
        var keys = new TreeSet<String>();
        replay.groups().forEach(group -> group.members().forEach(member -> keys.add(member.externalKey())));
        if (!keys.equals(truth.byAlertKey().keySet())) throw new IllegalArgumentException("Replay and truth must contain identical keys");
        var counts = new TreeMap<String, Long>();
        var rates = new TreeMap<String, Double>();
        var perFailure = new TreeMap<String, TreeMap<Long, Long>>();
        var perGroup = new TreeMap<Long, TreeMap<String, Long>>();
        var groups = new TreeMap<Long, ru.donskikh.incidenthub.experiment.replay.ReplayGroup>();
        var roots = new TreeMap<String, Long>();
        truth.failures().forEach(failure -> roots.put(failure.id(), failure.rootServiceId()));
        var visibleRoots = new TreeSet<String>();
        long predictedPairs = 0, truePositives = 0, noise = 0, absorbed = 0, mixed = 0, failureGroups = 0, undeterminedGroups = 0;
        for (var group : replay.groups()) {
            groups.put(group.id(), group);
            predictedPairs = Math.addExact(predictedPairs, pairs(group.members().size()));
            var failures = new TreeMap<String, Long>();
            long groupNoise = 0;
            for (var member : group.members()) {
                if (truth.byAlertKey().get(member.externalKey()) instanceof TruthLabel.FailureReference label) {
                    failures.merge(label.failureId(), 1L, Math::addExact);
                    perFailure.computeIfAbsent(label.failureId(), ignored -> new TreeMap<>()).merge(group.id(), 1L, Math::addExact);
                    if (member.alert().businessServiceId() == roots.get(label.failureId())) visibleRoots.add(label.failureId());
                } else groupNoise++;
            }
            perGroup.put(group.id(), failures);
            for (long size : failures.values()) truePositives = Math.addExact(truePositives, pairs(size));
            noise += groupNoise;
            if (!failures.isEmpty()) { failureGroups++; absorbed += groupNoise; }
            if (failures.size() >= 2) mixed++;
            if (group.snapshot().rootAssessment() instanceof RootAssessment.Undetermined) undeterminedGroups++;
        }
        long actualPairs = 0, fragmentation = 0, eligible = 0, excluded = 0, ranked = 0, undetermined = 0, hit1 = 0, hit3 = 0, hit5 = 0;
        for (var failure : perFailure.entrySet()) {
            long size = 0;
            long selected = 0, maximum = -1;
            for (var entry : failure.getValue().entrySet()) {
                size += entry.getValue();
                if (entry.getValue() > maximum) { selected = entry.getKey(); maximum = entry.getValue(); }
            }
            actualPairs = Math.addExact(actualPairs, pairs(size));
            fragmentation += failure.getValue().size();
            if (perGroup.get(selected).size() != 1) { excluded++; continue; }
            eligible++;
            if (groups.get(selected).snapshot().rootAssessment() instanceof RootAssessment.Ranked assessment) {
                ranked++;
                int position = -1;
                for (int index = 0; index < assessment.candidates().size(); index++) {
                    if (assessment.candidates().get(index).serviceId() == roots.get(failure.getKey())) { position = index + 1; break; }
                }
                if (position == 1) hit1++;
                if (position > 0 && position <= 3) hit3++;
                if (position > 0 && position <= 5) hit5++;
            } else undetermined++;
        }
        long fp = predictedPairs - truePositives, fn = actualPairs - truePositives;
        counts.put("pair_tp", truePositives); counts.put("pair_fp", fp); counts.put("pair_fn", fn);
        rates.put("pair_precision", ratio(truePositives, predictedPairs));
        rates.put("pair_recall", ratio(truePositives, actualPairs));
        rates.put("pair_f1", ratio(Math.multiplyExact(2, truePositives), Math.addExact(predictedPairs, actualPairs)));
        counts.put("alerts", (long) keys.size()); counts.put("groups", (long) groups.size());
        counts.put("noise_alerts", noise); counts.put("absorbed_noise_alerts", absorbed);
        counts.put("failure_groups", failureGroups); counts.put("false_merge_groups", mixed);
        rates.put("false_merge_rate", ratio(mixed, failureGroups)); rates.put("absorbed_noise_rate", ratio(absorbed, noise));
        rates.put("group_to_alert_ratio", ratio(groups.size(), keys.size()));
        rates.put("groups_per_visible_failure", ratio(fragmentation, perFailure.size()));
        counts.put("failures", (long) roots.size()); counts.put("visible_failures", (long) perFailure.size());
        counts.put("undetected_failures", (long) roots.size() - perFailure.size());
        counts.put("missing_root_failures", (long) roots.size() - visibleRoots.size());
        counts.put("visible_missing_root_failures", (long) perFailure.size() - visibleRoots.size());
        counts.put("localization_eligible", eligible); counts.put("localization_excluded_mixed", excluded);
        counts.put("localization_ranked", ranked); counts.put("localization_undetermined", undetermined);
        counts.put("localization_hits1", hit1); counts.put("localization_hits3", hit3); counts.put("localization_hits5", hit5);
        counts.put("undetermined_groups", undeterminedGroups);
        rates.put("ranked_coverage", ratio(ranked, eligible));
        rates.put("undetermined_failure_rate", ratio(undetermined, eligible));
        rates.put("undetermined_group_rate", ratio(undeterminedGroups, groups.size()));
        rates.put("localization_top1", ratio(hit1, ranked)); rates.put("localization_top1_strict", ratio(hit1, eligible));
        for (int k : new int[]{1, 3, 5}) {
            long hits = k == 1 ? hit1 : k == 3 ? hit3 : hit5;
            rates.put("localization_a" + k, ratio(hits, ranked));
            rates.put("localization_a" + k + "_strict", ratio(hits, eligible));
        }
        return new MetricsResult(counts, rates);
    }
    private long pairs(long size) { return Math.multiplyExact(size, size - 1) / 2; }
    private Double ratio(long numerator, long denominator) { return denominator == 0 ? null : (double) numerator / denominator; }
}
