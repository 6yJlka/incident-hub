package ru.donskikh.incidenthub.alert.correlation;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public final class TimeWindowCorrelator implements AlertCorrelator {

    private final Duration window;

    public TimeWindowCorrelator(Duration window) {
        this.window = Objects.requireNonNull(window, "window must not be null");
        if (window.isZero() || window.isNegative()) {
            throw new IllegalArgumentException("Correlation window must be positive");
        }
    }

    @Override
    public CorrelationDecision correlate(CorrelationAlert alert, CorrelationContext context) {
        Objects.requireNonNull(alert, "alert must not be null");
        Objects.requireNonNull(context, "context must not be null");
        OpenAlertGroupSnapshot selected = null;
        Duration closest = null;
        for (OpenAlertGroupSnapshot group : context.groups()) {
            Duration distance = distance(alert.occurredAt(), group).abs();
            if (distance.compareTo(window) <= 0
                    && (selected == null || distance.compareTo(closest) < 0
                    || (distance.equals(closest) && group.id() < selected.id()))) {
                selected = group;
                closest = distance;
            }
        }
        if (selected == null) {
            var evidence = new Evidence.EarliestAlert(alert.id(), alert.businessServiceId());
            return new CorrelationDecision.OpenGroup(ranked(evidence), List.of(evidence));
        }
        var incoming = new CorrelationGroupMember(alert.id(), alert.businessServiceId(), alert.occurredAt(),
                alert.signalType(), alert.severity());
        var earliest = java.util.stream.Stream.concat(selected.members().stream(), java.util.stream.Stream.of(incoming))
                .min(Comparator.comparing(CorrelationGroupMember::occurredAt)
                        .thenComparingLong(CorrelationGroupMember::alertId)).orElseThrow();
        return new CorrelationDecision.JoinGroup(selected.id(),
                ranked(new Evidence.EarliestAlert(earliest.alertId(), earliest.businessServiceId())),
                LinkStrength.TENTATIVE,
                List.of(new Evidence.TemporalProximity(alert.id(), distance(alert.occurredAt(), selected))));
    }

    private RootAssessment.Ranked ranked(Evidence.EarliestAlert evidence) {
        return new RootAssessment.Ranked(List.of(new RootCandidate(evidence.serviceId(), 1.0)), List.of(evidence));
    }

    private Duration distance(Instant occurredAt, OpenAlertGroupSnapshot group) {
        if (occurredAt.isBefore(group.firstAlertAt())) {
            return Duration.between(group.firstAlertAt(), occurredAt);
        }
        if (occurredAt.isAfter(group.lastAlertAt())) {
            return Duration.between(group.lastAlertAt(), occurredAt);
        }
        return Duration.ZERO;
    }
}
