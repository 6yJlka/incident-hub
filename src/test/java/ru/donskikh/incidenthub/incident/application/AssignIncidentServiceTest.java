package ru.donskikh.incidenthub.incident.application;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.donskikh.incidenthub.identity.User;
import ru.donskikh.incidenthub.identity.UserNotFoundException;
import ru.donskikh.incidenthub.identity.UserRepository;
import ru.donskikh.incidenthub.incident.Incident;
import ru.donskikh.incidenthub.incident.IncidentAssignmentNotAllowedException;
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
class AssignIncidentServiceTest {

    @Mock
    private IncidentRepository incidentRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AssignIncidentService service;

    @Test
    void assignsOpenIncident() {
        Incident incident = org.mockito.Mockito.mock(Incident.class);
        User assignee = org.mockito.Mockito.mock(User.class);
        when(incidentRepository.findById(42L)).thenReturn(Optional.of(incident));
        when(userRepository.findById(7L)).thenReturn(Optional.of(assignee));
        when(incident.getId()).thenReturn(42L);
        when(assignee.getId()).thenReturn(7L);
        when(incident.getStatus()).thenReturn(IncidentStatus.ASSIGNED);

        AssignIncidentResult result = service.assign(new AssignIncidentCommand(42L, 7L));

        verify(incident).assignTo(assignee);
        verify(incidentRepository, never()).save(incident);
        assertThat(result.incidentId()).isEqualTo(42L);
        assertThat(result.assigneeId()).isEqualTo(7L);
        assertThat(result.status()).isEqualTo(IncidentStatus.ASSIGNED);
    }

    @Test
    void reassignsAssignedIncident() {
        Incident incident = org.mockito.Mockito.mock(Incident.class);
        User newAssignee = org.mockito.Mockito.mock(User.class);
        when(incidentRepository.findById(42L)).thenReturn(Optional.of(incident));
        when(userRepository.findById(8L)).thenReturn(Optional.of(newAssignee));
        when(incident.getId()).thenReturn(42L);
        when(newAssignee.getId()).thenReturn(8L);
        when(incident.getStatus()).thenReturn(IncidentStatus.ASSIGNED);

        AssignIncidentResult result = service.assign(new AssignIncidentCommand(42L, 8L));

        verify(incident).assignTo(newAssignee);
        verify(incidentRepository, never()).save(incident);
        assertThat(result.incidentId()).isEqualTo(42L);
        assertThat(result.assigneeId()).isEqualTo(8L);
        assertThat(result.status()).isEqualTo(IncidentStatus.ASSIGNED);
    }

    @Test
    void throwsWhenIncidentDoesNotExistWithoutLoadingAssignee() {
        when(incidentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.assign(new AssignIncidentCommand(99L, 7L)))
                .isInstanceOf(IncidentNotFoundException.class)
                .hasMessage("Incident not found: 99");

        verifyNoInteractions(userRepository);
    }

    @Test
    void throwsWhenAssigneeDoesNotExist() {
        Incident incident = org.mockito.Mockito.mock(Incident.class);
        when(incidentRepository.findById(42L)).thenReturn(Optional.of(incident));
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.assign(new AssignIncidentCommand(42L, 99L)))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("User not found: 99");

        verify(incident, never()).assignTo(org.mockito.ArgumentMatchers.any(User.class));
    }

    @Test
    void propagatesDomainException() {
        Incident incident = org.mockito.Mockito.mock(Incident.class);
        User assignee = org.mockito.Mockito.mock(User.class);
        IncidentAssignmentNotAllowedException exception =
                new IncidentAssignmentNotAllowedException(42L, IncidentStatus.IN_PROGRESS);
        when(incidentRepository.findById(42L)).thenReturn(Optional.of(incident));
        when(userRepository.findById(7L)).thenReturn(Optional.of(assignee));
        doThrow(exception).when(incident).assignTo(assignee);

        assertThatThrownBy(() -> service.assign(new AssignIncidentCommand(42L, 7L)))
                .isSameAs(exception);

        verify(incidentRepository, never()).save(incident);
    }

    @Test
    void rejectsNullCommand() {
        assertThatThrownBy(() -> service.assign(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("command must not be null");

        verifyNoInteractions(incidentRepository, userRepository);
    }
}
