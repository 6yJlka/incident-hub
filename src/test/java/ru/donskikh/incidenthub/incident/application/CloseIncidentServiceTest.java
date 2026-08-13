package ru.donskikh.incidenthub.incident.application;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.donskikh.incidenthub.incident.Incident;
import ru.donskikh.incidenthub.incident.IncidentClosureNotAllowedException;
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
class CloseIncidentServiceTest {

    @Mock
    private IncidentRepository incidentRepository;

    @InjectMocks
    private CloseIncidentService service;

    @Test
    void closesResolvedIncident() {
        Incident incident = org.mockito.Mockito.mock(Incident.class);
        when(incidentRepository.findById(42L)).thenReturn(Optional.of(incident));
        when(incident.getId()).thenReturn(42L);
        when(incident.getStatus()).thenReturn(IncidentStatus.CLOSED);

        CloseIncidentResult result = service.close(new CloseIncidentCommand(42L));

        verify(incident).close();
        verify(incidentRepository, never()).save(incident);
        assertThat(result.incidentId()).isEqualTo(42L);
        assertThat(result.status()).isEqualTo(IncidentStatus.CLOSED);
    }

    @Test
    void throwsWhenIncidentDoesNotExist() {
        when(incidentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.close(new CloseIncidentCommand(99L)))
                .isInstanceOf(IncidentNotFoundException.class)
                .hasMessage("Incident not found: 99");
    }

    @Test
    void propagatesDomainExceptionForForbiddenStatus() {
        Incident incident = org.mockito.Mockito.mock(Incident.class);
        IncidentClosureNotAllowedException exception =
                new IncidentClosureNotAllowedException(42L, IncidentStatus.IN_PROGRESS);
        when(incidentRepository.findById(42L)).thenReturn(Optional.of(incident));
        doThrow(exception).when(incident).close();

        assertThatThrownBy(() -> service.close(new CloseIncidentCommand(42L)))
                .isSameAs(exception);

        verify(incidentRepository, never()).save(incident);
    }

    @Test
    void rejectsNullCommand() {
        assertThatThrownBy(() -> service.close(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("command must not be null");

        verifyNoInteractions(incidentRepository);
    }

    @Test
    void rejectsNonPositiveIncidentId() {
        assertThatThrownBy(() -> new CloseIncidentCommand(0L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("incidentId must be positive");
        assertThatThrownBy(() -> new CloseIncidentCommand(-1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("incidentId must be positive");

        verifyNoInteractions(incidentRepository);
    }
}
