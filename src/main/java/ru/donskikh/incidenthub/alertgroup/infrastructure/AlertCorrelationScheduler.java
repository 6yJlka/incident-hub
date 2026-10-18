package ru.donskikh.incidenthub.alertgroup.infrastructure;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.hibernate.StaleObjectStateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.donskikh.incidenthub.alertgroup.application.CorrelateAlertsCommand;
import ru.donskikh.incidenthub.alertgroup.application.CorrelateAlertsService;

@Component
@ConditionalOnProperty(prefix = "app.alerts.correlation", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AlertCorrelationScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(AlertCorrelationScheduler.class);

    private final CorrelateAlertsService service;
    private final AlertCorrelationProperties properties;

    public AlertCorrelationScheduler(CorrelateAlertsService service, AlertCorrelationProperties properties) {
        this.service = service;
        this.properties = properties;
    }

    @Scheduled(fixedDelayString = "${app.alerts.correlation.interval:PT5S}",
            initialDelayString = "${app.alerts.correlation.interval:PT5S}")
    public void tick() {
        try {
            service.process(new CorrelateAlertsCommand(properties.batchSize()));
        } catch (OptimisticLockingFailureException exception) {
            LOGGER.warn("Alert group {} changed concurrently; correlation batch will retry on the next tick",
                    groupId(exception));
        }
    }

    private Object groupId(Throwable exception) {
        for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
            if (cause instanceof ObjectOptimisticLockingFailureException optimistic) {
                if (optimistic.getIdentifier() != null) {
                    return optimistic.getIdentifier();
                }
            }
            if (cause instanceof StaleObjectStateException stale) {
                return stale.getIdentifier();
            }
        }
        return "unknown";
    }
}
