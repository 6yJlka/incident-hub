package ru.donskikh.incidenthub.experiment;

import org.junit.jupiter.api.Test;
import ru.donskikh.incidenthub.alert.*;
import ru.donskikh.incidenthub.alert.correlation.*;
import ru.donskikh.incidenthub.catalog.DependencyType;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import static org.assertj.core.api.Assertions.*;

class NaiveGraphRootCorrelatorTest {
    private final Duration window = Duration.ofMinutes(5);
    private final CorrelationAlert incoming = new CorrelationAlert(2, 2, Instant.EPOCH.plusSeconds(1), AlertSignalType.LATENCY, AlertSeverity.INFO);
    private final OpenAlertGroupSnapshot group = new OpenAlertGroupSnapshot(1,
            new RootAssessment.Undetermined("INSUFFICIENT_EVIDENCE"), Instant.EPOCH, Instant.EPOCH,
            List.of(new CorrelationGroupMember(1, 1, Instant.EPOCH, AlertSignalType.LATENCY, AlertSeverity.INFO)));
    private CorrelationContext context(boolean edge) {
        return new CorrelationContext(List.of(group), new DependencyGraphSnapshot(List.of(1L, 2L, 3L),
                edge ? List.of(new DependencyArc(2, 1, DependencyType.SYNC), new DependencyArc(1, 3, DependencyType.DATA)) : List.of()));
    }
    @Test
    void ranksDependencyHigherAndInducesGraphOnlyOnGroupServices() {
        var correlator = new NaiveGraphRootCorrelator(window, NaiveGraphRootParameters.defaults());
        var decision = correlator.correlate(incoming, context(true));
        var ranked = (RootAssessment.Ranked) decision.rootAssessment();
        assertThat(ranked.candidates()).extracting(RootCandidate::serviceId).containsExactly(1L, 2L);
        assertThat(ranked.candidates().getFirst().score()).isCloseTo(1.85 / 2.85, within(1e-10));
        assertThat(ranked.candidates().stream().mapToDouble(RootCandidate::score).sum()).isCloseTo(1, within(1e-12));
        assertThat(ranked.evidence()).hasSize(2).allMatch(e -> e instanceof Evidence.PageRankScore);
        assertThat(decision).isEqualTo(correlator.correlate(incoming, context(true)));
        var temporal = new TimeWindowCorrelator(window).correlate(incoming, context(true));
        assertThat(decision.linkEvidence()).isEqualTo(temporal.linkEvidence());
        assertThat(((CorrelationDecision.JoinGroup) decision).linkStrength()).isEqualTo(LinkStrength.TENTATIVE);
    }
    @Test
    void severityWeightsAffectRankAndTiesUseServiceIdWithEmptyGraph() {
        var critical = new CorrelationAlert(2, 2, incoming.occurredAt(), incoming.signalType(), AlertSeverity.CRITICAL);
        var correlator = new NaiveGraphRootCorrelator(window, NaiveGraphRootParameters.defaults());
        var ranked = (RootAssessment.Ranked) correlator.correlate(critical, context(false)).rootAssessment();
        assertThat(ranked.candidates()).containsExactly(new RootCandidate(2, .75), new RootCandidate(1, .25));
        ranked = (RootAssessment.Ranked) correlator.correlate(incoming, new CorrelationContext(List.of(group))).rootAssessment();
        assertThat(ranked.candidates()).containsExactly(new RootCandidate(1, .5), new RootCandidate(2, .5));
        var parameters = new NaiveGraphRootParameters(Map.of(AlertSeverity.CRITICAL, .1, AlertSeverity.WARNING, 2.0, AlertSeverity.INFO, 1.0), .85, 1e-12, 1000);
        assertThat(((RootAssessment.Ranked) new NaiveGraphRootCorrelator(window, parameters).correlate(critical, context(false)).rootAssessment()).candidates().getFirst().serviceId()).isEqualTo(1);
    }
    @Test
    void singletonHasUnitRankAndConvergenceLimitIsEnforced() {
        var parameters = NaiveGraphRootParameters.defaults();
        var open = new NaiveGraphRootCorrelator(window, parameters).correlate(incoming, new CorrelationContext(List.of()));
        assertThat(((RootAssessment.Ranked) open.rootAssessment()).candidates()).containsExactly(new RootCandidate(2, 1));
        assertThatIllegalStateException().isThrownBy(() -> new NaiveGraphRootCorrelator(window,
                new NaiveGraphRootParameters(parameters.severityWeights(), .85, 1e-15, 1)).correlate(incoming, context(true)));
        assertThatIllegalArgumentException().isThrownBy(() -> new NaiveGraphRootParameters(parameters.severityWeights(), 1, 1e-12, 100));
        assertThatIllegalArgumentException().isThrownBy(() -> new NaiveGraphRootParameters(Map.of(), .85, 1e-12, 100));
    }
    @Test
    void graphRejectsUnknownVerticesDuplicatesAndLoopsAndProtectsLists() {
        assertThatIllegalArgumentException().isThrownBy(() -> new DependencyArc(1, 1, DependencyType.SYNC));
        assertThatIllegalArgumentException().isThrownBy(() -> new DependencyGraphSnapshot(List.of(1L), List.of(new DependencyArc(2, 1, DependencyType.SYNC))));
        assertThatIllegalArgumentException().isThrownBy(() -> new DependencyGraphSnapshot(List.of(1L, 1L), List.of()));
        assertThatIllegalArgumentException().isThrownBy(() -> new DependencyGraphSnapshot(List.of(1L, 2L), List.of(new DependencyArc(2, 1, DependencyType.SYNC), new DependencyArc(2, 1, DependencyType.DATA))));
        assertThatThrownBy(() -> context(true).graph().edges().clear()).isInstanceOf(UnsupportedOperationException.class);
    }
}
