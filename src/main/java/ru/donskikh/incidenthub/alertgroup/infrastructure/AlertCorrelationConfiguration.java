package ru.donskikh.incidenthub.alertgroup.infrastructure;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import ru.donskikh.incidenthub.alert.correlation.AlertCorrelator;
import ru.donskikh.incidenthub.alert.correlation.TimeWindowCorrelator;

@Configuration(proxyBeanMethods = false)
@EnableScheduling
@EnableConfigurationProperties(AlertCorrelationProperties.class)
public class AlertCorrelationConfiguration {

    @Bean
    AlertCorrelator alertCorrelator(AlertCorrelationProperties properties) {
        return switch (properties.implementation()) {
            case TIME_WINDOW -> new TimeWindowCorrelator(properties.window());
        };
    }
}
