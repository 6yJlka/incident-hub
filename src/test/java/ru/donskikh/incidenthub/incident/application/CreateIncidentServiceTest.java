package ru.donskikh.incidenthub.incident.application;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.donskikh.incidenthub.identity.User;
import ru.donskikh.incidenthub.identity.UserNotFoundException;
import ru.donskikh.incidenthub.identity.UserRepository;
import ru.donskikh.incidenthub.incident.Incident;
import ru.donskikh.incidenthub.incident.IncidentCategory;
import ru.donskikh.incidenthub.incident.IncidentPriority;
import ru.donskikh.incidenthub.incident.IncidentRepository;
import ru.donskikh.incidenthub.incident.IncidentSource;
import ru.donskikh.incidenthub.incident.IncidentStatus;
import ru.donskikh.incidenthub.team.Team;
import ru.donskikh.incidenthub.team.TeamNotFoundException;
import ru.donskikh.incidenthub.team.TeamRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateIncidentServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private TeamRepository teamRepository;

    @Mock
    private IncidentRepository incidentRepository;

    @InjectMocks
    private CreateIncidentService service;

    @Test
    void createsManualIncidentWithoutResponsibleTeam() {
        User reporter = new User("reporter@example.com", "Reporter");
        Incident savedIncident = org.mockito.Mockito.mock(Incident.class);
        CreateIncidentCommand command = command(null);
        when(userRepository.findById(7L)).thenReturn(Optional.of(reporter));
        when(incidentRepository.save(any(Incident.class))).thenReturn(savedIncident);
        when(savedIncident.getId()).thenReturn(42L);
        when(savedIncident.getStatus()).thenReturn(IncidentStatus.OPEN);

        CreateIncidentResult result = service.create(command);

        ArgumentCaptor<Incident> incidentCaptor = ArgumentCaptor.forClass(Incident.class);
        verify(incidentRepository).save(incidentCaptor.capture());
        Incident incident = incidentCaptor.getValue();
        assertThat(incident.getCategory()).isEqualTo(IncidentCategory.INFRASTRUCTURE);
        assertThat(incident.getSource()).isEqualTo(IncidentSource.MANUAL);
        assertThat(incident.getReporter()).isSameAs(reporter);
        assertThat(incident.getResponsibleTeam()).isNull();
        assertThat(incident.getStatus()).isEqualTo(IncidentStatus.OPEN);
        assertThat(incident.getAssignee()).isNull();
        assertThat(result.incidentId()).isEqualTo(42L);
        assertThat(result.status()).isEqualTo(IncidentStatus.OPEN);
        verifyNoInteractions(teamRepository);
    }

    @Test
    void createsIncidentWithResponsibleTeamIncludingInactiveTeam() {
        User reporter = new User("reporter@example.com", "Reporter");
        Team team = new Team("Platform", "platform");
        team.deactivate();
        Incident savedIncident = org.mockito.Mockito.mock(Incident.class);
        when(userRepository.findById(7L)).thenReturn(Optional.of(reporter));
        when(teamRepository.findById(9L)).thenReturn(Optional.of(team));
        when(incidentRepository.save(any(Incident.class))).thenReturn(savedIncident);

        service.create(command(9L));

        ArgumentCaptor<Incident> incidentCaptor = ArgumentCaptor.forClass(Incident.class);
        verify(incidentRepository).save(incidentCaptor.capture());
        assertThat(incidentCaptor.getValue().getResponsibleTeam()).isSameAs(team);
        assertThat(incidentCaptor.getValue().getSource()).isEqualTo(IncidentSource.MANUAL);
        verify(teamRepository).findById(9L);
    }

    @Test
    void throwsWhenReporterDoesNotExist() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());
        CreateIncidentCommand command = new CreateIncidentCommand(
                "Database unavailable",
                "Production database does not accept connections",
                IncidentCategory.INFRASTRUCTURE,
                IncidentPriority.CRITICAL,
                99L,
                9L
        );

        assertThatThrownBy(() -> service.create(command))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("User not found: 99");

        verifyNoInteractions(teamRepository, incidentRepository);
    }

    @Test
    void throwsWhenResponsibleTeamDoesNotExistWithoutSavingIncident() {
        User reporter = new User("reporter@example.com", "Reporter");
        when(userRepository.findById(7L)).thenReturn(Optional.of(reporter));
        when(teamRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(command(99L)))
                .isInstanceOf(TeamNotFoundException.class)
                .hasMessage("Team not found: 99");

        verifyNoInteractions(incidentRepository);
    }

    @Test
    void rejectsNonPositiveResponsibleTeamId() {
        assertThatThrownBy(() -> command(0L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("responsibleTeamId must be positive");
        assertThatThrownBy(() -> command(-1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("responsibleTeamId must be positive");
    }

    @Test
    void doesNotSaveIncidentWhenDomainDataIsInvalid() {
        User reporter = new User("reporter@example.com", "Reporter");
        CreateIncidentCommand command = new CreateIncidentCommand(
                "   ",
                "Production database does not accept connections",
                IncidentCategory.INFRASTRUCTURE,
                IncidentPriority.CRITICAL,
                7L,
                null
        );
        when(userRepository.findById(7L)).thenReturn(Optional.of(reporter));

        assertThatThrownBy(() -> service.create(command))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("title must not be blank");

        verify(incidentRepository, never()).save(any(Incident.class));
    }

    private static CreateIncidentCommand command(Long responsibleTeamId) {
        return new CreateIncidentCommand(
                "Database unavailable",
                "Production database does not accept connections",
                IncidentCategory.INFRASTRUCTURE,
                IncidentPriority.CRITICAL,
                7L,
                responsibleTeamId
        );
    }
}
