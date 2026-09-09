package ru.donskikh.incidenthub.incident.application;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.donskikh.incidenthub.catalog.BusinessService;
import ru.donskikh.incidenthub.identity.User;
import ru.donskikh.incidenthub.incident.Incident;
import ru.donskikh.incidenthub.incident.IncidentNotFoundException;
import ru.donskikh.incidenthub.incident.IncidentPriority;
import ru.donskikh.incidenthub.incident.IncidentRepository;
import ru.donskikh.incidenthub.incident.IncidentSeverity;
import ru.donskikh.incidenthub.incident.IncidentSource;
import ru.donskikh.incidenthub.incident.IncidentStatus;
import ru.donskikh.incidenthub.team.Team;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetIncidentServiceTest {

    @Mock
    private IncidentRepository incidentRepository;

    @InjectMocks
    private GetIncidentService service;

    @Test
    void returnsIncidentWithReporterAssigneeAndResponsibleTeam() {
        Incident incident = org.mockito.Mockito.mock(Incident.class);
        BusinessService affectedService = org.mockito.Mockito.mock(BusinessService.class);
        User reporter = org.mockito.Mockito.mock(User.class);
        User assignee = org.mockito.Mockito.mock(User.class);
        Team team = org.mockito.Mockito.mock(Team.class);
        Instant createdAt = Instant.parse("2026-08-01T10:15:30Z");
        Instant updatedAt = Instant.parse("2026-08-02T11:20:35Z");
        when(incidentRepository.findById(42L)).thenReturn(Optional.of(incident));
        when(incident.getId()).thenReturn(42L);
        when(incident.getTitle()).thenReturn("Database unavailable");
        when(incident.getDescription()).thenReturn("Production database does not accept connections");
        when(incident.getAffectedService()).thenReturn(affectedService);
        when(incident.getSource()).thenReturn(IncidentSource.MANUAL);
        when(incident.getPriority()).thenReturn(IncidentPriority.CRITICAL);
        when(incident.getSeverity()).thenReturn(IncidentSeverity.SEV1);
        when(incident.getStatus()).thenReturn(IncidentStatus.IN_PROGRESS);
        when(incident.getReporter()).thenReturn(reporter);
        when(incident.getResponsibleTeam()).thenReturn(team);
        when(incident.getAssignee()).thenReturn(assignee);
        when(incident.getCreatedAt()).thenReturn(createdAt);
        when(incident.getUpdatedAt()).thenReturn(updatedAt);
        when(affectedService.getId()).thenReturn(11L);
        when(affectedService.getCode()).thenReturn("BILLING");
        when(affectedService.getName()).thenReturn("Billing");
        when(reporter.getId()).thenReturn(7L);
        when(reporter.getDisplayName()).thenReturn("Reporter");
        when(team.getId()).thenReturn(9L);
        when(team.getName()).thenReturn("Platform");
        when(team.getCode()).thenReturn("PLATFORM");
        when(assignee.getId()).thenReturn(8L);
        when(assignee.getDisplayName()).thenReturn("Assignee");

        GetIncidentResult result = service.get(42L);

        assertThat(result.id()).isEqualTo(42L);
        assertThat(result.affectedServiceId()).isEqualTo(11L);
        assertThat(result.affectedServiceCode()).isEqualTo("BILLING");
        assertThat(result.affectedServiceName()).isEqualTo("Billing");
        assertThat(result.source()).isEqualTo(IncidentSource.MANUAL);
        assertThat(result.severity()).isEqualTo(IncidentSeverity.SEV1);
        assertThat(result.reporterId()).isEqualTo(7L);
        assertThat(result.reporterDisplayName()).isEqualTo("Reporter");
        assertThat(result.responsibleTeamId()).isEqualTo(9L);
        assertThat(result.responsibleTeamName()).isEqualTo("Platform");
        assertThat(result.responsibleTeamCode()).isEqualTo("PLATFORM");
        assertThat(result.assigneeId()).isEqualTo(8L);
        assertThat(result.assigneeDisplayName()).isEqualTo("Assignee");
        assertThat(result.createdAt()).isEqualTo(createdAt);
        assertThat(result.updatedAt()).isEqualTo(updatedAt);
    }

    @Test
    void returnsNullAssociationFieldsWhenIncidentIsUnassignedAndUnrouted() {
        Incident incident = org.mockito.Mockito.mock(Incident.class);
        BusinessService affectedService = org.mockito.Mockito.mock(BusinessService.class);
        User reporter = org.mockito.Mockito.mock(User.class);
        when(incidentRepository.findById(42L)).thenReturn(Optional.of(incident));
        when(incident.getAffectedService()).thenReturn(affectedService);
        when(incident.getReporter()).thenReturn(reporter);

        GetIncidentResult result = service.get(42L);

        assertThat(result.assigneeId()).isNull();
        assertThat(result.assigneeDisplayName()).isNull();
        assertThat(result.responsibleTeamId()).isNull();
        assertThat(result.responsibleTeamName()).isNull();
        assertThat(result.responsibleTeamCode()).isNull();
    }

    @Test
    void throwsWhenIncidentDoesNotExist() {
        when(incidentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(99L))
                .isInstanceOf(IncidentNotFoundException.class)
                .hasMessage("Incident not found: 99");

        verify(incidentRepository, times(1)).findById(99L);
    }

    @Test
    void rejectsNonPositiveIncidentId() {
        assertThatThrownBy(() -> service.get(0L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("incidentId must be positive");
        assertThatThrownBy(() -> service.get(-1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("incidentId must be positive");

        verifyNoInteractions(incidentRepository);
    }
}
