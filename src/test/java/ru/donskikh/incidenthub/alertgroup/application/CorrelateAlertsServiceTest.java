package ru.donskikh.incidenthub.alertgroup.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;
import ru.donskikh.incidenthub.alert.Alert;
import ru.donskikh.incidenthub.alert.AlertRepository;
import ru.donskikh.incidenthub.alert.AlertSeverity;
import ru.donskikh.incidenthub.alert.AlertSignalType;
import ru.donskikh.incidenthub.alert.correlation.*;
import ru.donskikh.incidenthub.alert.correlation.AlertCorrelator;
import ru.donskikh.incidenthub.alert.correlation.CorrelationContext;
import ru.donskikh.incidenthub.alert.correlation.CorrelationDecision;
import ru.donskikh.incidenthub.alertgroup.AlertGroup;
import ru.donskikh.incidenthub.alertgroup.AlertGroupRepository;
import ru.donskikh.incidenthub.alertgroup.infrastructure.AlertCorrelationProperties;
import ru.donskikh.incidenthub.catalog.BusinessService;
import ru.donskikh.incidenthub.catalog.BusinessServiceRepository;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static ru.donskikh.incidenthub.CorrelationTestData.ranked;

@ExtendWith(MockitoExtension.class)
class CorrelateAlertsServiceTest {

    @Mock
    private AlertRepository alerts;

    @Mock
    private AlertGroupRepository groups;

    @Mock
    private BusinessServiceRepository services;

    @Mock
    private AlertCorrelator correlator;

    private CorrelateAlertsService processor;

    @BeforeEach
    void setUp() {
        processor = new CorrelateAlertsService(alerts, groups, services, correlator,
                new AlertCorrelationProperties(false, Duration.ofSeconds(5), 500,
                        AlertCorrelationProperties.Implementation.TIME_WINDOW, Duration.ofMinutes(5)),
                mock(jakarta.persistence.EntityManager.class));
    }

    @Test
    void skipsBatchBeforeReadingAnyDataWhenLockIsBusy() {
        assertThat(processor.process(new CorrelateAlertsCommand(10)))
                .isEqualTo(new CorrelationBatchResult(false, 0, 0));
        verifyNoInteractions(groups, services, correlator);
        verify(alerts, never()).findAllByGroupIsNullOrderByOccurredAtAscIdAsc(any());
    }

    @Test
    void emptyTickDoesNotReadGroupsOrComposition() {
        when(alerts.tryAcquireCorrelationLock()).thenReturn(true);
        assertThat(processor.process(new CorrelateAlertsCommand(10)))
                .isEqualTo(new CorrelationBatchResult(true, 0, 0));
        verifyNoInteractions(groups, services, correlator);
        verify(alerts, never()).findCorrelationMembers(any());
    }

    @Test
    void updatesCompositionAfterEachAlertAndPersistsRootFromDecision() {
        Instant time = Instant.parse("2026-10-18T12:00:00Z");
        var observedService = mock(BusinessService.class);
        var chosenRoot = mock(BusinessService.class);
        when(observedService.getId()).thenReturn(11L);
        when(chosenRoot.getId()).thenReturn(99L);
        Alert first = alert(1L, time, observedService);
        Alert second = alert(2L, time.plusSeconds(60), observedService);
        when(alerts.tryAcquireCorrelationLock()).thenReturn(true);
        when(alerts.findAllByGroupIsNullOrderByOccurredAtAscIdAsc(any(Pageable.class)))
                .thenReturn(List.of(first, second));
        when(services.getReferenceById(99L)).thenReturn(chosenRoot);
        when(groups.save(any())).thenAnswer(invocation -> {
            AlertGroup group = invocation.getArgument(0);
            ReflectionTestUtils.setField(group, "id", 7L);
            return group;
        });
        when(alerts.attachToGroup(anyLong(), anyLong(), nullable(LinkStrength.class), any())).thenReturn(1);
        when(correlator.correlate(any(), any())).thenAnswer(invocation -> {
            CorrelationContext context = invocation.getArgument(1);
            if (context.groups().isEmpty()) {
                return new CorrelationDecision.OpenGroup(ranked(99), List.of(new Evidence.Migrated()));
            }
            assertThat(context.groups().getFirst().members()).hasSize(1);
            assertThat(context.groups().getFirst().members().getFirst().businessServiceId()).isEqualTo(11);
            return new CorrelationDecision.JoinGroup(7, ranked(99), LinkStrength.TENTATIVE, List.of(new Evidence.Migrated()));
        });

        assertThat(processor.process(new CorrelateAlertsCommand(10)))
                .isEqualTo(new CorrelationBatchResult(true, 2, 1));

        verify(groups).findCorrelationCandidates(time.minusSeconds(300), time.plusSeconds(360));
        verify(alerts).attachToGroup(1, 7, null, List.of(new Evidence.Migrated()));
        verify(alerts).attachToGroup(2, 7, LinkStrength.TENTATIVE, List.of(new Evidence.Migrated()));
        verify(alerts, never()).findCorrelationMembers(any());
    }

    @Test
    void rejectsNullCommandBeforeDatabaseAccess() {
        assertThatThrownBy(() -> processor.process(null)).isInstanceOf(NullPointerException.class);
        verifyNoInteractions(alerts, groups, services, correlator);
    }

    private Alert alert(Long id, Instant time, BusinessService service) {
        Alert alert = mock(Alert.class);
        when(alert.getId()).thenReturn(id);
        when(alert.getOccurredAt()).thenReturn(time);
        when(alert.getBusinessService()).thenReturn(service);
        when(alert.getSignalType()).thenReturn(AlertSignalType.LATENCY);
        when(alert.getSeverity()).thenReturn(AlertSeverity.WARNING);
        return alert;
    }
}
