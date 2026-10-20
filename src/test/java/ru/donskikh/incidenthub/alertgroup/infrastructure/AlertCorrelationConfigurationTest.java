package ru.donskikh.incidenthub.alertgroup.infrastructure;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import ru.donskikh.incidenthub.alert.AlertSeverity;
import ru.donskikh.incidenthub.alert.AlertSignalType;
import ru.donskikh.incidenthub.alert.correlation.AlertCorrelator;
import ru.donskikh.incidenthub.alert.correlation.CorrelationAlert;
import ru.donskikh.incidenthub.alert.correlation.CorrelationContext;
import ru.donskikh.incidenthub.alert.correlation.CorrelationDecision;
import ru.donskikh.incidenthub.alert.correlation.Evidence;
import ru.donskikh.incidenthub.alert.correlation.RootAssessment;
import ru.donskikh.incidenthub.alert.correlation.RootCandidate;
import ru.donskikh.incidenthub.alert.correlation.TimeWindowCorrelator;
import ru.donskikh.incidenthub.alertgroup.application.CorrelateAlertsCommand;
import ru.donskikh.incidenthub.alertgroup.application.CorrelateAlertsService;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class AlertCorrelationConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(AlertCorrelationConfiguration.class);

    @Test
    void createsConfiguredPureCorrelatorWithDefaultWindow() {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context.getBean(AlertCorrelator.class)).isInstanceOf(TimeWindowCorrelator.class);
            assertThat(context.getBean(AlertCorrelationProperties.class).window()).isEqualTo(Duration.ofMinutes(5));
            assertThat(context.getBean(AlertCorrelator.class).correlate(
                    new CorrelationAlert(1, 42, Instant.now(), AlertSignalType.LATENCY, AlertSeverity.INFO),
                    new CorrelationContext(List.of()))).isEqualTo(new CorrelationDecision.OpenGroup(new RootAssessment.Ranked(List.of(new RootCandidate(42, 1.0)), List.of(new Evidence.EarliestAlert(1, 42))), List.of(new Evidence.EarliestAlert(1, 42))));
        });
    }

    @Test
    void rejectsUnsupportedImplementationAndInvalidSettings() {
        for (String property : List.of("implementation=unknown", "window=PT0S", "interval=-PT1S", "batch-size=0")) {
            runner.withPropertyValues("app.alerts.correlation." + property)
                    .run(context -> assertThat(context).hasFailed());
        }
    }

    @Test
    void schedulerDelegatesToTransactionalServiceWithConfiguredBatchSize() {
        var service = mock(CorrelateAlertsService.class);
        var properties = new AlertCorrelationProperties(true, Duration.ofSeconds(5), 17,
                AlertCorrelationProperties.Implementation.TIME_WINDOW, Duration.ofMinutes(5));
        new AlertCorrelationScheduler(service, properties).tick();
        verify(service).process(new CorrelateAlertsCommand(17));
    }
}
