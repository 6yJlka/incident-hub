package ru.donskikh.incidenthub.experiment;

import org.junit.jupiter.api.Test;
import ru.donskikh.incidenthub.alert.*;
import ru.donskikh.incidenthub.alert.correlation.*;
import ru.donskikh.incidenthub.experiment.metrics.*;
import ru.donskikh.incidenthub.experiment.replay.*;
import ru.donskikh.incidenthub.experiment.scenario.*;
import java.time.Instant;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

class ExperimentMetricsTest {
    private RootAssessment root(long id) { return id == 0 ? new RootAssessment.Undetermined("INSUFFICIENT_EVIDENCE")
            : new RootAssessment.Ranked(List.of(new RootCandidate(id, 1)), List.of(new Evidence.Migrated())); }
    private MetricsResult evaluate(List<List<String>> partitions, Map<Long, RootAssessment> assessments, boolean invisibleFailure) {
        var groups = new ArrayList<ReplayGroup>();
        var labels = new TreeMap<String, TruthLabel>();
        var failures = new TreeMap<String, Failure>();
        long alertId = 0;
        for (var keys : partitions) {
            long groupId = groups.size() + 1;
            var members = new ArrayList<ReplayMember>();
            for (String key : keys) {
                long service = switch (key) { case "A1" -> 1; case "A2", "A3" -> 2; case "B1" -> 3; case "B2" -> 4; default -> 5; };
                var alert = new CorrelationAlert(++alertId, service, Instant.EPOCH, AlertSignalType.LATENCY, AlertSeverity.INFO);
                members.add(new ReplayMember(key, alert, LinkStrength.TENTATIVE, List.of(new Evidence.Migrated())));
                if (key.startsWith("N")) labels.put(key, new TruthLabel.Noise());
                else {
                    String failure = key.substring(0, 1);
                    labels.put(key, new TruthLabel.FailureReference(failure));
                    failures.put(failure, new Failure(failure, failure.equals("A") ? 1 : 3, Instant.EPOCH));
                }
            }
            var assessment = assessments.getOrDefault(groupId, root(keys.getFirst().startsWith("B") ? 3 : 1));
            groups.add(new ReplayGroup(groupId, new OpenAlertGroupSnapshot(groupId, assessment, Instant.EPOCH, Instant.EPOCH,
                    members.stream().map(m -> new CorrelationGroupMember(m.alert().id(), m.alert().businessServiceId(), Instant.EPOCH, m.alert().signalType(), m.alert().severity())).toList()), members));
        }
        if (invisibleFailure) failures.put("Z", new Failure("Z", 9, Instant.EPOCH));
        return new ExperimentMetrics().calculate(new ReplayResult(groups), new GroundTruth(labels, List.copyOf(failures.values())));
    }
    @Test
    void idealPartitionHasPerfectPairAndLocalizationMetrics() {
        var result = evaluate(List.of(List.of("A1", "A2"), List.of("B1", "B2"), List.of("N1"), List.of("N2")), Map.of(), false);
        assertThat(result.counts()).containsEntry("pair_tp", 2L).containsEntry("pair_fp", 0L).containsEntry("pair_fn", 0L);
        assertThat(result.rates()).containsEntry("pair_precision", 1.0).containsEntry("pair_recall", 1.0).containsEntry("pair_f1", 1.0)
                .containsEntry("localization_top1", 1.0).containsEntry("localization_a3_strict", 1.0).containsEntry("ranked_coverage", 1.0)
                .containsEntry("groups_per_visible_failure", 1.0).containsEntry("absorbed_noise_rate", 0.0);
        assertThat(result.rates().get("group_to_alert_ratio")).isCloseTo(4.0 / 6, within(1e-12));
    }
    @Test
    void allInOneCountsNoisePairsAsFalseAndExcludesMixedLocalization() {
        var result = evaluate(List.of(List.of("A1", "A2", "B1", "B2", "N1", "N2")), Map.of(), false);
        assertThat(result.counts()).containsEntry("pair_tp", 2L).containsEntry("pair_fp", 13L).containsEntry("pair_fn", 0L)
                .containsEntry("false_merge_groups", 1L).containsEntry("localization_excluded_mixed", 2L);
        assertThat(result.rates()).containsEntry("false_merge_rate", 1.0).containsEntry("absorbed_noise_rate", 1.0);
        assertThat(result.rates().get("pair_f1")).isCloseTo(4.0 / 17, within(1e-12));
        assertThat(result.rates().get("localization_top1_strict")).isNull();
    }
    @Test
    void singletonsExposeFragmentationAndUndefinedPrecision() {
        var result = evaluate(List.of(List.of("A1"), List.of("A2"), List.of("B1"), List.of("B2")), Map.of(), false);
        assertThat(result.rates()).containsEntry("pair_recall", 0.0).containsEntry("pair_f1", 0.0)
                .containsEntry("groups_per_visible_failure", 2.0).containsEntry("group_to_alert_ratio", 1.0);
        assertThat(result.rates().get("pair_precision")).isNull();
    }
    @Test
    void primaryGroupUsesLargestCountThenSmallestIdAndRequiresPurity() {
        var result = evaluate(List.of(List.of("A1"), List.of("A2", "A3", "N1")), Map.of(1L, root(2), 2L, root(1)), false);
        assertThat(result.rates()).containsEntry("localization_top1", 1.0).containsEntry("groups_per_visible_failure", 2.0);
        result = evaluate(List.of(List.of("A1"), List.of("A2")), Map.of(1L, root(2), 2L, root(1)), false);
        assertThat(result.rates()).containsEntry("localization_top1", 0.0);
        result = evaluate(List.of(List.of("A1", "B1"), List.of("A2")), Map.of(), false);
        assertThat(result.counts()).containsEntry("localization_eligible", 0L).containsEntry("localization_excluded_mixed", 2L);
    }
    @Test
    void conditionalAndStrictAccuracyAreReportedWithCoverage() {
        var result = evaluate(List.of(List.of("A1", "A2"), List.of("B1", "B2")), Map.of(2L, root(0)), false);
        assertThat(result.rates()).containsEntry("localization_top1", 1.0).containsEntry("localization_top1_strict", .5)
                .containsEntry("localization_a3", 1.0).containsEntry("localization_a3_strict", .5).containsEntry("ranked_coverage", .5)
                .containsEntry("undetermined_failure_rate", .5);
        assertThat(result.counts()).containsEntry("localization_undetermined", 1L).containsEntry("undetermined_groups", 1L);
        result = evaluate(List.of(List.of("A1")), Map.of(1L, root(0)), false);
        assertThat(result.rates().get("localization_top1")).isNull();
        assertThat(result.rates()).containsEntry("localization_top1_strict", 0.0).containsEntry("ranked_coverage", 0.0);
    }
    @Test
    void localizationCanNameInvisibleRootAndUndetectedFailuresRemainSeparate() {
        var assessment = new RootAssessment.Ranked(List.of(new RootCandidate(2, .8), new RootCandidate(1, .2)), List.of(new Evidence.Migrated()));
        var result = evaluate(List.of(List.of("A2")), Map.of(1L, assessment), true);
        assertThat(result.counts()).containsEntry("missing_root_failures", 2L).containsEntry("visible_missing_root_failures", 1L)
                .containsEntry("undetected_failures", 1L).containsEntry("visible_failures", 1L);
        assertThat(result.rates()).containsEntry("localization_top1", 0.0).containsEntry("localization_a3", 1.0);
        result = evaluate(List.of(List.of("A2")), Map.of(1L, root(1)), false);
        assertThat(result.rates()).containsEntry("localization_top1", 1.0);
    }
    @Test
    void noiseOnlyAndEmptyInputsHaveExplicitUndefinedRatios() {
        var result = evaluate(List.of(List.of("N1", "N2")), Map.of(), true);
        assertThat(result.counts()).containsEntry("pair_fp", 1L);
        assertThat(result.rates().get("pair_recall")).isNull();
        assertThat(result.rates()).containsEntry("pair_precision", 0.0).containsEntry("absorbed_noise_rate", 0.0);
        result = evaluate(List.of(), Map.of(), true);
        assertThat(result.rates().values()).allMatch(Objects::isNull);
        assertThat(result.counts()).containsEntry("undetected_failures", 1L);
    }
    @Test
    void keyMismatchIsRejected() {
        assertThatIllegalArgumentException().isThrownBy(() -> new ExperimentMetrics().calculate(new ReplayResult(List.of()),
                new GroundTruth(new TreeMap<>(Map.of("extra", new TruthLabel.Noise())), List.of())));
    }
}
