package ru.donskikh.incidenthub.alert;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;
import java.util.Objects;

@ConfigurationProperties("app.alerts")
public record AlertProperties(
        @DefaultValue("500") int maxBatchSize,
        @DefaultValue("PT1M") Duration maxClockSkew
) {
    public AlertProperties {
        Objects.requireNonNull(maxClockSkew, "maxClockSkew must not be null");
        if (maxBatchSize <= 0) {
            throw new IllegalArgumentException("Alert maximum batch size must be positive");
        }
        if (maxClockSkew.isNegative()) {
            throw new IllegalArgumentException("Alert maximum clock skew must not be negative");
        }
    }
}
