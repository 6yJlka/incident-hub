package ru.donskikh.incidenthub.alertgroup.infrastructure;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ru.donskikh.incidenthub.alertgroup.application.CorrelateAlertsCommand;
import ru.donskikh.incidenthub.alertgroup.application.CorrelateAlertsService;

@Component
@ConditionalOnProperty(prefix = "app.alerts.correlation", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AlertCorrelationScheduler {

    private final CorrelateAlertsService service;
    private final AlertCorrelationProperties properties;

    public AlertCorrelationScheduler(CorrelateAlertsService service, AlertCorrelationProperties properties) {
        this.service = service;
        this.properties = properties;
    }

    @Scheduled(fixedDelayString = "${app.alerts.correlation.interval:PT5S}",
            initialDelayString = "${app.alerts.correlation.interval:PT5S}")
    public void tick() {
        service.process(new CorrelateAlertsCommand(properties.batchSize()));
    }
}
