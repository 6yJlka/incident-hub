package ru.donskikh.incidenthub.alert.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.donskikh.incidenthub.alert.AlertRepository;
import ru.donskikh.incidenthub.alert.AlertProperties;
import ru.donskikh.incidenthub.alert.AlertSignalType;
import ru.donskikh.incidenthub.alert.AlertSeverity;
import ru.donskikh.incidenthub.alert.NewAlert;
import ru.donskikh.incidenthub.catalog.BusinessService;
import ru.donskikh.incidenthub.catalog.BusinessServiceRepository;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AcceptAlertsServiceTest {
    private static final Instant NOW = Instant.parse("2026-10-18T12:00:00Z");
    @Mock
    private AlertRepository alertRepository;

    @Mock
    private BusinessServiceRepository businessServiceRepository;

    private AcceptAlertsService service;

    @BeforeEach
    void setUp() {
        service = new AcceptAlertsService(alertRepository, businessServiceRepository,
                new AlertProperties(500, Duration.ofMinutes(1)), Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void acceptsNewAlertWithServerReceiptTime() {
        prepareService();
        var input = alert("one", "SERVICE");
        when(alertRepository.findIdsByKeys(any())).thenReturn(Map.of()).thenReturn(Map.of(input.key(), 10L));
        when(alertRepository.insertIfAbsent(any())).thenReturn(Map.of(input.key(), 10L));

        var result = service.accept(new AcceptAlertsCommand(List.of(input)));

        assertThat(result.results()).containsExactly(new AlertAcceptanceResult("one", AlertAcceptanceStatus.ACCEPTED, 10L, null));
        verify(alertRepository).insertIfAbsent(List.of(new NewAlert(input.key(), 7,
                input.signalType(), input.severity(), input.occurredAt(), NOW, input.description())));
    }

    @Test
    void returnsExistingIdAndIgnoresChangedServiceForDuplicate() {
        var input = alert("one", "UNKNOWN");
        when(alertRepository.findIdsByKeys(any())).thenReturn(Map.of(input.key(), 10L));

        var result = service.accept(new AcceptAlertsCommand(List.of(input)));

        assertThat(result.results()).containsExactly(new AlertAcceptanceResult("one", AlertAcceptanceStatus.DUPLICATE, 10L, null));
        verifyNoInteractions(businessServiceRepository);
        verify(alertRepository, never()).insertIfAbsent(any());
    }

    @Test
    void marksSubsequentOccurrencesInTheBatchAsDuplicates() {
        prepareService();
        var input = alert("one", "SERVICE");
        when(alertRepository.findIdsByKeys(any())).thenReturn(Map.of()).thenReturn(Map.of(input.key(), 10L));
        when(alertRepository.insertIfAbsent(any())).thenReturn(Map.of(input.key(), 10L));

        var result = service.accept(new AcceptAlertsCommand(List.of(input, alert("one", "UNKNOWN"))));

        assertThat(result.results()).extracting(AlertAcceptanceResult::status)
                .containsExactly(AlertAcceptanceStatus.ACCEPTED, AlertAcceptanceStatus.DUPLICATE);
        assertThat(result.results()).extracting(AlertAcceptanceResult::alertId).containsOnly(10L);
        verify(alertRepository).insertIfAbsent(argThat(alerts -> alerts.size() == 1));
    }

    @Test
    void rejectsUnknownServiceWithoutInserting() {
        var result = service.accept(new AcceptAlertsCommand(List.of(alert("one", "UNKNOWN"))));

        assertThat(result.results()).containsExactly(new AlertAcceptanceResult(
                "one", AlertAcceptanceStatus.REJECTED, null, AlertRejectionReason.UNKNOWN_SERVICE));
        verify(alertRepository, never()).insertIfAbsent(any());
    }

    @Test
    void preservesInputOrderInMixedBatchAndDoesNotReserveRejectedKey() {
        prepareService();
        var valid = alert("new", "SERVICE");
        var existing = alert("existing", "UNKNOWN");
        when(alertRepository.findIdsByKeys(any())).thenReturn(
                Map.of(existing.key(), 9L)).thenReturn(Map.of(valid.key(), 10L));
        when(alertRepository.insertIfAbsent(any())).thenReturn(Map.of(valid.key(), 10L));

        var result = service.accept(new AcceptAlertsCommand(List.of(
                alert("new", "UNKNOWN"), existing, valid, valid, alert("bad", "UNKNOWN"))));

        assertThat(result.results()).extracting(AlertAcceptanceResult::externalId)
                .containsExactly("new", "existing", "new", "new", "bad");
        assertThat(result.results()).extracting(AlertAcceptanceResult::status).containsExactly(
                AlertAcceptanceStatus.REJECTED, AlertAcceptanceStatus.DUPLICATE, AlertAcceptanceStatus.ACCEPTED,
                AlertAcceptanceStatus.DUPLICATE, AlertAcceptanceStatus.REJECTED);
    }

    @Test
    void returnsConcurrentWinnerAsDuplicate() {
        prepareService();
        var input = alert("one", "SERVICE");
        when(alertRepository.findIdsByKeys(any())).thenReturn(Map.of()).thenReturn(Map.of(input.key(), 99L));

        var result = service.accept(new AcceptAlertsCommand(List.of(input)));

        assertThat(result.results()).containsExactly(new AlertAcceptanceResult("one", AlertAcceptanceStatus.DUPLICATE, 99L, null));
    }

    @Test
    void acceptsAlertExactlyAtClockSkewBoundary() {
        prepareService();
        var input = alertAt("boundary", NOW.plusSeconds(60));
        when(alertRepository.findIdsByKeys(any())).thenReturn(Map.of()).thenReturn(Map.of(input.key(), 10L));
        when(alertRepository.insertIfAbsent(any())).thenReturn(Map.of(input.key(), 10L));

        var result = service.accept(new AcceptAlertsCommand(List.of(input)));

        assertThat(result.results()).containsExactly(new AlertAcceptanceResult(
                "boundary", AlertAcceptanceStatus.ACCEPTED, 10L, null));
        verify(alertRepository).insertIfAbsent(List.of(new NewAlert(input.key(), 7,
                input.signalType(), input.severity(), input.occurredAt(), NOW, input.description())));
    }

    @Test
    void rejectsAlertOneNanosecondBeyondClockSkewWithoutInserting() {
        BusinessService businessService = mock(BusinessService.class);
        when(businessService.getCode()).thenReturn("SERVICE");
        when(businessServiceRepository.findAllByCodeIn(any())).thenReturn(List.of(businessService));

        var result = service.accept(new AcceptAlertsCommand(List.of(
                alertAt("future", NOW.plusSeconds(60).plusNanos(1)))));

        assertThat(result.results()).containsExactly(new AlertAcceptanceResult(
                "future", AlertAcceptanceStatus.REJECTED, null, AlertRejectionReason.OCCURRED_IN_FUTURE));
        verify(alertRepository, never()).insertIfAbsent(any());
    }

    @Test
    void acceptsFirstValidOccurrenceAfterFutureOccurrenceWithSameKey() {
        prepareService();
        var valid = alertAt("same", NOW);
        when(alertRepository.findIdsByKeys(any())).thenReturn(Map.of()).thenReturn(Map.of(valid.key(), 10L));
        when(alertRepository.insertIfAbsent(any())).thenReturn(Map.of(valid.key(), 10L));

        var result = service.accept(new AcceptAlertsCommand(List.of(
                alertAt("same", NOW.plusSeconds(61)), valid)));

        assertThat(result.results()).containsExactly(
                new AlertAcceptanceResult("same", AlertAcceptanceStatus.REJECTED, null, AlertRejectionReason.OCCURRED_IN_FUTURE),
                new AlertAcceptanceResult("same", AlertAcceptanceStatus.ACCEPTED, 10L, null));
        verify(alertRepository).insertIfAbsent(List.of(new NewAlert(valid.key(), 7,
                valid.signalType(), valid.severity(), valid.occurredAt(), NOW, valid.description())));
    }

    @Test
    void acceptsValidItemsInMixedBatchAndChecksDuplicatesBeforeFutureTime() {
        prepareService();
        var valid = alertAt("new", NOW);
        var existing = alertAt("existing", Instant.MAX);
        when(alertRepository.findIdsByKeys(any())).thenReturn(Map.of(existing.key(), 9L))
                .thenReturn(Map.of(valid.key(), 10L));
        when(alertRepository.insertIfAbsent(any())).thenReturn(Map.of(valid.key(), 10L));

        var result = service.accept(new AcceptAlertsCommand(List.of(
                alertAt("future", NOW.plusSeconds(61)), valid, existing,
                alertAt("new", Instant.MAX), alert("unknown", "UNKNOWN"))));

        assertThat(result.results()).containsExactly(
                new AlertAcceptanceResult("future", AlertAcceptanceStatus.REJECTED, null, AlertRejectionReason.OCCURRED_IN_FUTURE),
                new AlertAcceptanceResult("new", AlertAcceptanceStatus.ACCEPTED, 10L, null),
                new AlertAcceptanceResult("existing", AlertAcceptanceStatus.DUPLICATE, 9L, null),
                new AlertAcceptanceResult("new", AlertAcceptanceStatus.DUPLICATE, 10L, null),
                new AlertAcceptanceResult("unknown", AlertAcceptanceStatus.REJECTED, null, AlertRejectionReason.UNKNOWN_SERVICE));
        verify(alertRepository).insertIfAbsent(argThat(alerts -> alerts.size() == 1));
    }

    @Test
    void usesConfiguredClockSkewIncludingZero() {
        prepareService();
        service = new AcceptAlertsService(alertRepository, businessServiceRepository,
                new AlertProperties(500, Duration.ZERO), Clock.fixed(NOW, ZoneOffset.UTC));
        var valid = alertAt("now", NOW);
        when(alertRepository.findIdsByKeys(any())).thenReturn(Map.of()).thenReturn(Map.of(valid.key(), 10L));
        when(alertRepository.insertIfAbsent(any())).thenReturn(Map.of(valid.key(), 10L));

        var result = service.accept(new AcceptAlertsCommand(List.of(
                alertAt("future", NOW.plusNanos(1)), valid)));

        assertThat(result.results()).containsExactly(
                new AlertAcceptanceResult("future", AlertAcceptanceStatus.REJECTED, null, AlertRejectionReason.OCCURRED_IN_FUTURE),
                new AlertAcceptanceResult("now", AlertAcceptanceStatus.ACCEPTED, 10L, null));
    }

    @Test
    void rejectsNullAndOversizedCommandsBeforeDatabaseAccess() {
        assertThatThrownBy(() -> service.accept(null)).isInstanceOf(NullPointerException.class);
        var smallService = new AcceptAlertsService(alertRepository, businessServiceRepository,
                new AlertProperties(1, Duration.ofMinutes(1)), Clock.systemUTC());
        assertThatThrownBy(() -> smallService.accept(new AcceptAlertsCommand(
                List.of(alert("one", "SERVICE"), alert("two", "SERVICE")))))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new AcceptAlertsCommand(List.of())).isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(alertRepository, businessServiceRepository);
    }

    private void prepareService() {
        BusinessService businessService = mock(BusinessService.class);
        when(businessService.getId()).thenReturn(7L);
        when(businessService.getCode()).thenReturn("SERVICE");
        when(businessServiceRepository.findAllByCodeIn(any())).thenReturn(List.of(businessService));
    }

    private IncomingAlert alert(String id, String code) {
        return new IncomingAlert("prometheus", id, code, AlertSignalType.LATENCY,
                AlertSeverity.WARNING, NOW.minusSeconds(60), "Latency increased");
    }

    private IncomingAlert alertAt(String id, Instant occurredAt) {
        return new IncomingAlert("prometheus", id, "SERVICE", AlertSignalType.LATENCY,
                AlertSeverity.WARNING, occurredAt, "Latency increased");
    }
}
