package ru.donskikh.incidenthub.incident.application;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.donskikh.incidenthub.audit.IncidentAuditEventType;
import ru.donskikh.incidenthub.audit.IncidentAuditService;
import ru.donskikh.incidenthub.incident.Incident;
import ru.donskikh.incidenthub.incident.IncidentCancellationNotAllowedException;
import ru.donskikh.incidenthub.incident.IncidentNotFoundException;
import ru.donskikh.incidenthub.incident.IncidentRepository;
import ru.donskikh.incidenthub.incident.IncidentStatus;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CancelIncidentServiceTest {

    @Mock
    private IncidentRepository incidentRepository;

    @Mock
    private IncidentAuditService auditService;

    @InjectMocks
    private CancelIncidentService service;

    @Test
    void cancelsOpenIncident() {
        Incident incident = cancelledIncident(42L, IncidentStatus.OPEN);
        when(incidentRepository.findById(42L)).thenReturn(Optional.of(incident));

        CancelIncidentResult result = service.cancel(new CancelIncidentCommand(42L));

        assertSuccessfulCancellation(incident, IncidentStatus.OPEN, result);
    }

    @Test
    void cancelsAssignedIncident() {
        Incident incident = cancelledIncident(42L, IncidentStatus.ASSIGNED);
        when(incidentRepository.findById(42L)).thenReturn(Optional.of(incident));

        CancelIncidentResult result = service.cancel(new CancelIncidentCommand(42L));

        assertSuccessfulCancellation(incident, IncidentStatus.ASSIGNED, result);
    }

    @Test
    void cancelsInProgressIncident() {
        Incident incident = cancelledIncident(42L, IncidentStatus.IN_PROGRESS);
        when(incidentRepository.findById(42L)).thenReturn(Optional.of(incident));

        CancelIncidentResult result = service.cancel(new CancelIncidentCommand(42L));

        assertSuccessfulCancellation(incident, IncidentStatus.IN_PROGRESS, result);
    }

    @Test
    void throwsWhenIncidentDoesNotExist() {
        when(incidentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.cancel(new CancelIncidentCommand(99L)))
                .isInstanceOf(IncidentNotFoundException.class)
                .hasMessage("Incident not found: 99");

        verifyNoInteractions(auditService);
    }

    @Test
    void propagatesDomainExceptionForResolvedIncident() {
        IncidentCancellationNotAllowedException exception =
                new IncidentCancellationNotAllowedException(42L, IncidentStatus.RESOLVED);
        Incident incident = forbiddenIncident(exception);
        when(incidentRepository.findById(42L)).thenReturn(Optional.of(incident));

        assertThatThrownBy(() -> service.cancel(new CancelIncidentCommand(42L)))
                .isSameAs(exception);

        verify(incidentRepository, never()).save(incident);
        verifyNoInteractions(auditService);
    }

    @Test
    void propagatesDomainExceptionForRepeatedCancellation() {
        IncidentCancellationNotAllowedException exception =
                new IncidentCancellationNotAllowedException(42L, IncidentStatus.CANCELLED);
        Incident incident = forbiddenIncident(exception);
        when(incidentRepository.findById(42L)).thenReturn(Optional.of(incident));

        assertThatThrownBy(() -> service.cancel(new CancelIncidentCommand(42L)))
                .isSameAs(exception);

        verify(incidentRepository, never()).save(incident);
        verifyNoInteractions(auditService);
    }

    @Test
    void rejectsNullCommand() {
        assertThatThrownBy(() -> service.cancel(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("command must not be null");

        verifyNoInteractions(incidentRepository, auditService);
    }

    @Test
    void rejectsNonPositiveIncidentId() {
        assertThatThrownBy(() -> new CancelIncidentCommand(0L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("incidentId must be positive");
        assertThatThrownBy(() -> new CancelIncidentCommand(-1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("incidentId must be positive");

        verifyNoInteractions(incidentRepository, auditService);
    }

    private Incident cancelledIncident(long incidentId, IncidentStatus fromStatus) {
        Incident incident = org.mockito.Mockito.mock(Incident.class);
        when(incident.getId()).thenReturn(incidentId);
        when(incident.getStatus()).thenReturn(fromStatus, IncidentStatus.CANCELLED);
        return incident;
    }

    private Incident forbiddenIncident(IncidentCancellationNotAllowedException exception) {
        Incident incident = org.mockito.Mockito.mock(Incident.class);
        doThrow(exception).when(incident).cancel();
        return incident;
    }

    private void assertSuccessfulCancellation(
            Incident incident,
            IncidentStatus fromStatus,
            CancelIncidentResult result
    ) {
        verify(incident).cancel();
        verify(incidentRepository, never()).save(incident);
        verify(auditService).record(
                42L, IncidentAuditEventType.CANCELLED, fromStatus, IncidentStatus.CANCELLED
        );
        assertThat(result.incidentId()).isEqualTo(42L);
        assertThat(result.status()).isEqualTo(IncidentStatus.CANCELLED);
    }
}
