package ru.donskikh.incidenthub.alertgroup.infrastructure;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;
import java.util.Objects;

@ConfigurationProperties("app.alerts.correlation")
public record AlertCorrelationProperties(
        @DefaultValue("true") boolean enabled,
        @DefaultValue("PT5S") Duration interval,
        @DefaultValue("500") int batchSize,
        @DefaultValue("time-window") Implementation implementation,
        @DefaultValue("PT5M") Duration window
) {
    public AlertCorrelationProperties {
        Objects.requireNonNull(interval, "interval must not be null");
        Objects.requireNonNull(window, "window must not be null");
        Objects.requireNonNull(implementation, "implementation must not be null");
        if (interval.isZero() || interval.isNegative() || window.isZero() || window.isNegative()) {
            throw new IllegalArgumentException("Correlation interval and window must be positive");
        }
        if (batchSize <= 0) {
            throw new IllegalArgumentException("Correlation batch size must be positive");
        }
    }

    public enum Implementation {
        TIME_WINDOW
    }
}
