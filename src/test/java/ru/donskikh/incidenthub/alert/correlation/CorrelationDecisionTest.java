package ru.donskikh.incidenthub.alert.correlation;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CorrelationDecisionTest {
    private final List<Evidence> evidence = List.of(new Evidence.Migrated());

    @Test
    void rejectsEmptyUnsortedAndDuplicateCandidates() {
        for (var candidates : List.of(List.<RootCandidate>of(),
                List.of(new RootCandidate(1, .1), new RootCandidate(2, .9)),
                List.of(new RootCandidate(1, .9), new RootCandidate(1, .1)))) {
            assertThatThrownBy(() -> new RootAssessment.Ranked(candidates, evidence))
                    .isInstanceOf(IllegalArgumentException.class);
        }
        for (double score : new double[]{Double.NaN, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY}) {
            assertThatThrownBy(() -> new RootCandidate(1, score)).isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Test
    void permitsTiedScoresAndCopiesCollections() {
        var candidates = new ArrayList<>(List.of(new RootCandidate(1, .9), new RootCandidate(2, .9)));
        var reasons = new ArrayList<>(evidence);
        var ranked = new RootAssessment.Ranked(candidates, reasons);
        var join = new CorrelationDecision.JoinGroup(1, ranked, LinkStrength.SUPPORTED, reasons);
        candidates.clear();
        reasons.clear();
        assertThat(ranked.candidates()).hasSize(2);
        assertThat(join.linkEvidence()).containsExactly(new Evidence.Migrated());
        assertThatThrownBy(() -> ranked.candidates().clear()).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> ranked.evidence().clear()).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> join.linkEvidence().clear()).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void requiresEvidenceForOpeningJoiningAndRanking() {
        var assessment = new RootAssessment.Undetermined("INSUFFICIENT_EVIDENCE");
        assertThatThrownBy(() -> new CorrelationDecision.OpenGroup(assessment, List.of()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CorrelationDecision.JoinGroup(1, assessment, LinkStrength.TENTATIVE, List.of()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new RootAssessment.Ranked(List.of(new RootCandidate(1, 1)), List.of()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CorrelationDecision.JoinGroup(1, assessment, null, evidence))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void validatesReasonCodesAndDependencyPaths() {
        for (String reason : List.of("", " ", "not enough data", "insufficient_evidence")) {
            assertThatThrownBy(() -> new RootAssessment.Undetermined(reason)).isInstanceOf(IllegalArgumentException.class);
        }
        assertThat(new RootAssessment.Undetermined("FUTURE_REASON").reason()).isEqualTo("FUTURE_REASON");
        assertThatThrownBy(() -> new Evidence.DependencyPath(List.of(1L))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Evidence.DependencyPath(List.of(1L, 0L))).isInstanceOf(IllegalArgumentException.class);
        var ids = new ArrayList<>(List.of(1L, 2L));
        var path = new Evidence.DependencyPath(ids);
        ids.clear();
        assertThat(path.serviceIds()).containsExactly(1L, 2L);
    }
}
