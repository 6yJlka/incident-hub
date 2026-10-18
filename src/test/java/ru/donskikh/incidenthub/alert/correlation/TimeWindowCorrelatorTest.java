package ru.donskikh.incidenthub.alert.correlation;

import org.junit.jupiter.api.Test;
import ru.donskikh.incidenthub.alert.AlertSeverity;
import ru.donskikh.incidenthub.alert.AlertSignalType;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TimeWindowCorrelatorTest {

    private static final Instant BASE = Instant.parse("2026-10-18T12:00:00Z");
    private final TimeWindowCorrelator correlator = new TimeWindowCorrelator(Duration.ofMinutes(5));

    @Test
    void createsFirstGroupWithoutSpring() {
        assertThat(correlator.correlate(alert(1, 42, 0), new CorrelationContext(List.of())))
                .isEqualTo(new CorrelationDecision.OpenGroup(42));
    }

    @Test
    void usesSlidingWindowAndIncludesBoundary() {
        var group = group(7, 42, 0, 4);
        assertThat(correlator.correlate(alert(2, 99, 9), context(group)))
                .isEqualTo(new CorrelationDecision.JoinGroup(7, 42));
        assertThat(correlator.correlate(alert(3, 99, 10), context(group)))
                .isEqualTo(new CorrelationDecision.OpenGroup(99));
    }

    @Test
    void acceptsLateAlertInsideLongSessionEvenWhenFarFromLastAlert() {
        assertThat(correlator.correlate(alert(2, 99, 10), context(group(7, 42, 0, 60))))
                .isEqualTo(new CorrelationDecision.JoinGroup(7, 42));
    }

    @Test
    void updatesRootOnlyForStrictlyEarlierAlertWithinWindow() {
        var group = group(7, 42, 0, 60);
        assertThat(correlator.correlate(alert(2, 99, -5), context(group)))
                .isEqualTo(new CorrelationDecision.JoinGroup(7, 99));
        assertThat(correlator.correlate(alert(3, 99, -6), context(group)))
                .isEqualTo(new CorrelationDecision.OpenGroup(99));
        assertThat(correlator.correlate(alert(4, 99, 0), context(group)))
                .isEqualTo(new CorrelationDecision.JoinGroup(7, 42));
    }

    @Test
    void selectsNearestIntervalWithoutMergingOverlappingWindows() {
        var first = group(7, 42, 0, 2);
        var second = group(8, 99, 6, 8);
        assertThat(correlator.correlate(alert(3, 123, 5), context(first, second)))
                .isEqualTo(new CorrelationDecision.JoinGroup(8, 123));
        assertThat(correlator.correlate(alert(4, 123, 4), context(second, first)))
                .isEqualTo(new CorrelationDecision.JoinGroup(7, 42));
    }

    @Test
    void prefersContainingIntervalAndBreaksZeroDistanceTiesById() {
        var first = group(7, 42, 0, 60);
        var second = group(8, 99, 20, 30);
        assertThat(correlator.correlate(alert(3, 123, 25), context(second, first)))
                .isEqualTo(new CorrelationDecision.JoinGroup(7, 42));
    }

    @Test
    void snapshotsDefensivelyCopyGroupComposition() {
        var members = new ArrayList<CorrelationGroupMember>();
        members.add(new CorrelationGroupMember(42, BASE, AlertSignalType.ERROR_RATE, AlertSeverity.CRITICAL));
        var snapshot = new OpenAlertGroupSnapshot(7, 42, BASE, BASE, members);
        var groups = new ArrayList<OpenAlertGroupSnapshot>();
        groups.add(snapshot);
        var context = new CorrelationContext(groups);
        members.clear();
        groups.clear();
        assertThat(context.groups()).hasSize(1);
        assertThat(context.groups().getFirst().members()).hasSize(1);
        assertThatThrownBy(() -> context.groups().clear()).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> snapshot.members().clear()).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void rejectsInvalidWindowAndIdentifiers() {
        assertThatThrownBy(() -> new TimeWindowCorrelator(Duration.ZERO)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TimeWindowCorrelator(Duration.ofSeconds(-1))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> alert(0, 42, 0)).isInstanceOf(IllegalArgumentException.class);
    }

    private CorrelationAlert alert(long id, long serviceId, long minute) {
        return new CorrelationAlert(id, serviceId, BASE.plusSeconds(minute * 60),
                AlertSignalType.LATENCY, AlertSeverity.WARNING);
    }

    private OpenAlertGroupSnapshot group(long id, long serviceId, long first, long last) {
        return new OpenAlertGroupSnapshot(id, serviceId, BASE.plusSeconds(first * 60), BASE.plusSeconds(last * 60),
                List.of(new CorrelationGroupMember(serviceId, BASE.plusSeconds(first * 60),
                        AlertSignalType.LATENCY, AlertSeverity.WARNING)));
    }

    private CorrelationContext context(OpenAlertGroupSnapshot... groups) {
        return new CorrelationContext(List.of(groups));
    }
}
