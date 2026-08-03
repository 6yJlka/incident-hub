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
import ru.donskikh.incidenthub.incident.IncidentPriority;
import ru.donskikh.incidenthub.incident.IncidentRepository;
import ru.donskikh.incidenthub.incident.IncidentStatus;

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
    private IncidentRepository incidentRepository;

    @InjectMocks
    private CreateIncidentService service;

    @Test
    void createsIncidentForExistingReporter() {
        User reporter = new User("reporter@example.com", "Reporter");
        Incident savedIncident = org.mockito.Mockito.mock(Incident.class);
        CreateIncidentCommand command = new CreateIncidentCommand(
                "Database unavailable",
                "Production database does not accept connections",
                "Infrastructure",
                IncidentPriority.CRITICAL,
                7L
        );

        when(userRepository.findById(7L)).thenReturn(Optional.of(reporter));
        when(incidentRepository.save(any(Incident.class))).thenReturn(savedIncident);
        when(savedIncident.getId()).thenReturn(42L);
        when(savedIncident.getStatus()).thenReturn(IncidentStatus.OPEN);

        CreateIncidentResult result = service.create(command);

        ArgumentCaptor<Incident> incidentCaptor = ArgumentCaptor.forClass(Incident.class);
        verify(incidentRepository).save(incidentCaptor.capture());
        Incident incident = incidentCaptor.getValue();

        assertThat(incident.getTitle()).isEqualTo("Database unavailable");
        assertThat(incident.getDescription()).isEqualTo("Production database does not accept connections");
        assertThat(incident.getCategory()).isEqualTo("Infrastructure");
        assertThat(incident.getPriority()).isEqualTo(IncidentPriority.CRITICAL);
        assertThat(incident.getReporter()).isSameAs(reporter);
        assertThat(incident.getStatus()).isEqualTo(IncidentStatus.OPEN);
        assertThat(incident.getAssignee()).isNull();
        assertThat(result.incidentId()).isEqualTo(42L);
        assertThat(result.status()).isEqualTo(IncidentStatus.OPEN);
        verify(userRepository).findById(7L);
    }

    @Test
    void throwsWhenReporterDoesNotExist() {
        CreateIncidentCommand command = new CreateIncidentCommand(
                "Database unavailable",
                "Production database does not accept connections",
                "Infrastructure",
                IncidentPriority.CRITICAL,
                99L
        );
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(command))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("User not found: 99");

        verify(userRepository).findById(99L);
        verifyNoInteractions(incidentRepository);
    }

    @Test
    void doesNotSaveIncidentWhenDomainDataIsInvalid() {
        User reporter = new User("reporter@example.com", "Reporter");
        CreateIncidentCommand command = new CreateIncidentCommand(
                "   ",
                "Production database does not accept connections",
                "Infrastructure",
                IncidentPriority.CRITICAL,
                7L
        );
        when(userRepository.findById(7L)).thenReturn(Optional.of(reporter));

        assertThatThrownBy(() -> service.create(command))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("title must not be blank");

        verify(userRepository).findById(7L);
        verify(incidentRepository, never()).save(any(Incident.class));
    }
}
