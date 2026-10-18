package ru.donskikh.incidenthub.alert;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties("app.alerts")
public record AlertProperties(@DefaultValue("500") int maxBatchSize) {
    public AlertProperties {
        if (maxBatchSize <= 0) {
            throw new IllegalArgumentException("Alert maximum batch size must be positive");
        }
    }
}
