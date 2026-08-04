package ru.donskikh.incidenthub.incident.application;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.donskikh.incidenthub.identity.User;
import ru.donskikh.incidenthub.incident.Incident;
import ru.donskikh.incidenthub.incident.IncidentNotFoundException;
import ru.donskikh.incidenthub.incident.IncidentPriority;
import ru.donskikh.incidenthub.incident.IncidentRepository;
import ru.donskikh.incidenthub.incident.IncidentStatus;

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
    void returnsIncidentWithReporterAndAssignee() {
        Incident incident = org.mockito.Mockito.mock(Incident.class);
        User reporter = org.mockito.Mockito.mock(User.class);
        User assignee = org.mockito.Mockito.mock(User.class);
        Instant createdAt = Instant.parse("2026-08-01T10:15:30Z");
        Instant updatedAt = Instant.parse("2026-08-02T11:20:35Z");

        when(incidentRepository.findById(42L)).thenReturn(Optional.of(incident));
        when(incident.getId()).thenReturn(42L);
        when(incident.getTitle()).thenReturn("Database unavailable");
        when(incident.getDescription()).thenReturn("Production database does not accept connections");
        when(incident.getCategory()).thenReturn("Infrastructure");
        when(incident.getPriority()).thenReturn(IncidentPriority.CRITICAL);
        when(incident.getStatus()).thenReturn(IncidentStatus.IN_PROGRESS);
        when(incident.getReporter()).thenReturn(reporter);
        when(incident.getAssignee()).thenReturn(assignee);
        when(incident.getCreatedAt()).thenReturn(createdAt);
        when(incident.getUpdatedAt()).thenReturn(updatedAt);
        when(reporter.getId()).thenReturn(7L);
        when(reporter.getDisplayName()).thenReturn("Reporter");
        when(assignee.getId()).thenReturn(8L);
        when(assignee.getDisplayName()).thenReturn("Assignee");

        GetIncidentResult result = service.get(42L);

        assertThat(result.id()).isEqualTo(42L);
        assertThat(result.title()).isEqualTo("Database unavailable");
        assertThat(result.description()).isEqualTo("Production database does not accept connections");
        assertThat(result.category()).isEqualTo("Infrastructure");
        assertThat(result.priority()).isEqualTo(IncidentPriority.CRITICAL);
        assertThat(result.status()).isEqualTo(IncidentStatus.IN_PROGRESS);
        assertThat(result.reporterId()).isEqualTo(7L);
        assertThat(result.reporterDisplayName()).isEqualTo("Reporter");
        assertThat(result.assigneeId()).isEqualTo(8L);
        assertThat(result.assigneeDisplayName()).isEqualTo("Assignee");
        assertThat(result.createdAt()).isEqualTo(createdAt);
        assertThat(result.updatedAt()).isEqualTo(updatedAt);
    }

    @Test
    void returnsNullAssigneeFieldsWhenIncidentIsUnassigned() {
        Incident incident = org.mockito.Mockito.mock(Incident.class);
        User reporter = org.mockito.Mockito.mock(User.class);

        when(incidentRepository.findById(42L)).thenReturn(Optional.of(incident));
        when(incident.getReporter()).thenReturn(reporter);

        GetIncidentResult result = service.get(42L);

        assertThat(result.assigneeId()).isNull();
        assertThat(result.assigneeDisplayName()).isNull();
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
