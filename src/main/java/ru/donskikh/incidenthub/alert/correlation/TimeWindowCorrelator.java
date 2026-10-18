package ru.donskikh.incidenthub.alert.correlation;

import java.time.Duration;
import java.time.Instant;
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
            Duration distance = distance(alert.occurredAt(), group);
            if (distance.compareTo(window) <= 0
                    && (selected == null || distance.compareTo(closest) < 0
                    || (distance.equals(closest) && group.id() < selected.id()))) {
                selected = group;
                closest = distance;
            }
        }
        if (selected == null) {
            return new CorrelationDecision.OpenGroup(alert.businessServiceId());
        }
        long rootServiceId = alert.occurredAt().isBefore(selected.firstAlertAt())
                ? alert.businessServiceId() : selected.probableRootServiceId();
        return new CorrelationDecision.JoinGroup(selected.id(), rootServiceId);
    }

    private Duration distance(Instant occurredAt, OpenAlertGroupSnapshot group) {
        if (occurredAt.isBefore(group.firstAlertAt())) {
            return Duration.between(occurredAt, group.firstAlertAt());
        }
        if (occurredAt.isAfter(group.lastAlertAt())) {
            return Duration.between(group.lastAlertAt(), occurredAt);
        }
        return Duration.ZERO;
    }
}
