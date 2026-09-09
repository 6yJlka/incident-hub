package ru.donskikh.incidenthub.incident.application;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.donskikh.incidenthub.audit.IncidentAuditEventType;
import ru.donskikh.incidenthub.audit.IncidentAuditService;
import ru.donskikh.incidenthub.catalog.BusinessService;
import ru.donskikh.incidenthub.catalog.BusinessServiceNotFoundException;
import ru.donskikh.incidenthub.catalog.BusinessServiceRepository;
import ru.donskikh.incidenthub.catalog.ServiceTier;
import ru.donskikh.incidenthub.identity.User;
import ru.donskikh.incidenthub.identity.UserNotFoundException;
import ru.donskikh.incidenthub.identity.UserRepository;
import ru.donskikh.incidenthub.incident.Incident;
import ru.donskikh.incidenthub.incident.IncidentPriority;
import ru.donskikh.incidenthub.incident.IncidentRepository;
import ru.donskikh.incidenthub.incident.IncidentSeverity;
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
    private BusinessServiceRepository businessServiceRepository;

    @Mock
    private TeamRepository teamRepository;

    @Mock
    private IncidentRepository incidentRepository;

    @Mock
    private IncidentAuditService auditService;

    @InjectMocks
    private CreateIncidentService service;

    @Test
    void derivesResponsibleTeamFromAffectedService() {
        User reporter = new User("reporter@example.com", "Reporter");
        Team ownerTeam = new Team("Payments", "PAYMENTS");
        BusinessService affectedService = affectedService(ownerTeam);
        Incident savedIncident = org.mockito.Mockito.mock(Incident.class);
        when(userRepository.findById(7L)).thenReturn(Optional.of(reporter));
        when(businessServiceRepository.findById(11L)).thenReturn(Optional.of(affectedService));
        when(incidentRepository.save(any(Incident.class))).thenReturn(savedIncident);
        when(savedIncident.getId()).thenReturn(42L);
        when(savedIncident.getStatus()).thenReturn(IncidentStatus.OPEN);

        CreateIncidentResult result = service.create(command(null));

        ArgumentCaptor<Incident> incidentCaptor = ArgumentCaptor.forClass(Incident.class);
        verify(incidentRepository).save(incidentCaptor.capture());
        Incident incident = incidentCaptor.getValue();
        assertThat(incident.getAffectedService()).isSameAs(affectedService);
        assertThat(incident.getSeverity()).isEqualTo(IncidentSeverity.SEV1);
        assertThat(incident.getSource()).isEqualTo(IncidentSource.MANUAL);
        assertThat(incident.getReporter()).isSameAs(reporter);
        assertThat(incident.getResponsibleTeam()).isSameAs(ownerTeam);
        assertThat(incident.getStatus()).isEqualTo(IncidentStatus.OPEN);
        assertThat(incident.getAssignee()).isNull();
        assertThat(result.incidentId()).isEqualTo(42L);
        assertThat(result.status()).isEqualTo(IncidentStatus.OPEN);
        verify(auditService).record(42L, IncidentAuditEventType.CREATED, null, IncidentStatus.OPEN);
        verifyNoInteractions(teamRepository);
    }

    @Test
    void explicitResponsibleTeamOverridesAffectedServiceOwner() {
        User reporter = new User("reporter@example.com", "Reporter");
        Team ownerTeam = new Team("Payments", "PAYMENTS");
        Team explicitTeam = new Team("Platform", "PLATFORM");
        explicitTeam.deactivate();
        BusinessService affectedService = affectedService(ownerTeam);
        Incident savedIncident = org.mockito.Mockito.mock(Incident.class);
        when(userRepository.findById(7L)).thenReturn(Optional.of(reporter));
        when(businessServiceRepository.findById(11L)).thenReturn(Optional.of(affectedService));
        when(teamRepository.findById(9L)).thenReturn(Optional.of(explicitTeam));
        when(incidentRepository.save(any(Incident.class))).thenReturn(savedIncident);

        service.create(command(9L));

        ArgumentCaptor<Incident> incidentCaptor = ArgumentCaptor.forClass(Incident.class);
        verify(incidentRepository).save(incidentCaptor.capture());
        assertThat(incidentCaptor.getValue().getAffectedService()).isSameAs(affectedService);
        assertThat(incidentCaptor.getValue().getResponsibleTeam()).isSameAs(explicitTeam);
        verify(teamRepository).findById(9L);
    }

    @Test
    void throwsWhenReporterDoesNotExist() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());
        CreateIncidentCommand command = new CreateIncidentCommand(
                "Database unavailable",
                "Production database does not accept connections",
                11L,
                IncidentPriority.CRITICAL,
                IncidentSeverity.SEV1,
                99L,
                9L
        );

        assertThatThrownBy(() -> service.create(command))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("User not found: 99");

        verifyNoInteractions(businessServiceRepository, teamRepository, incidentRepository, auditService);
    }

    @Test
    void throwsWhenAffectedServiceDoesNotExist() {
        User reporter = new User("reporter@example.com", "Reporter");
        when(userRepository.findById(7L)).thenReturn(Optional.of(reporter));
        when(businessServiceRepository.findById(11L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(command(null)))
                .isInstanceOf(BusinessServiceNotFoundException.class)
                .hasMessage("Business service not found: 11");

        verifyNoInteractions(teamRepository, incidentRepository, auditService);
    }

    @Test
    void throwsWhenResponsibleTeamDoesNotExistWithoutSavingIncident() {
        User reporter = new User("reporter@example.com", "Reporter");
        BusinessService affectedService = affectedService(new Team("Payments", "PAYMENTS"));
        when(userRepository.findById(7L)).thenReturn(Optional.of(reporter));
        when(businessServiceRepository.findById(11L)).thenReturn(Optional.of(affectedService));
        when(teamRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(command(99L)))
                .isInstanceOf(TeamNotFoundException.class)
                .hasMessage("Team not found: 99");

        verifyNoInteractions(incidentRepository, auditService);
    }

    @Test
    void rejectsNonPositiveAffectedServiceId() {
        assertThatThrownBy(() -> command(0L, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("affectedServiceId must be positive");
        assertThatThrownBy(() -> command(-1L, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("affectedServiceId must be positive");
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
        BusinessService affectedService = affectedService(new Team("Payments", "PAYMENTS"));
        CreateIncidentCommand command = new CreateIncidentCommand(
                "   ",
                "Production database does not accept connections",
                11L,
                IncidentPriority.CRITICAL,
                IncidentSeverity.SEV1,
                7L,
                null
        );
        when(userRepository.findById(7L)).thenReturn(Optional.of(reporter));
        when(businessServiceRepository.findById(11L)).thenReturn(Optional.of(affectedService));

        assertThatThrownBy(() -> service.create(command))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("title must not be blank");

        verify(incidentRepository, never()).save(any(Incident.class));
        verifyNoInteractions(auditService);
    }

    private static CreateIncidentCommand command(Long responsibleTeamId) {
        return command(11L, responsibleTeamId);
    }

    private static CreateIncidentCommand command(long affectedServiceId, Long responsibleTeamId) {
        return new CreateIncidentCommand(
                "Database unavailable",
                "Production database does not accept connections",
                affectedServiceId,
                IncidentPriority.CRITICAL,
                IncidentSeverity.SEV1,
                7L,
                responsibleTeamId
        );
    }

    private static BusinessService affectedService(Team ownerTeam) {
        return new BusinessService(
                "BILLING",
                "Billing",
                "Billing service",
                ownerTeam,
                ServiceTier.TIER_1
        );
    }
}
