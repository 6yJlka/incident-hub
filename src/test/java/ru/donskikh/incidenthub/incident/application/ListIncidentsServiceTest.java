package ru.donskikh.incidenthub.incident.application;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import ru.donskikh.incidenthub.identity.User;
import ru.donskikh.incidenthub.incident.Incident;
import ru.donskikh.incidenthub.incident.IncidentCategory;
import ru.donskikh.incidenthub.incident.IncidentPriority;
import ru.donskikh.incidenthub.incident.IncidentRepository;
import ru.donskikh.incidenthub.incident.IncidentSource;
import ru.donskikh.incidenthub.incident.IncidentStatus;
import ru.donskikh.incidenthub.team.Team;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListIncidentsServiceTest {

    @Mock
    private IncidentRepository incidentRepository;

    @InjectMocks
    private ListIncidentsService service;

    @Test
    void rejectsNullQuery() {
        assertThatThrownBy(() -> service.execute(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("query must not be null");
        verifyNoInteractions(incidentRepository);
    }

    @Test
    void passesSpecificationAndStablePageRequestAndReturnsMetadata() {
        ListIncidentsQuery query = new ListIncidentsQuery(
                1, 10, IncidentStatus.OPEN, IncidentPriority.HIGH,
                IncidentCategory.INFRASTRUCTURE, IncidentSource.AUTOMATIC, 9L
        );
        when(incidentRepository.findAll(
                ArgumentMatchers.<Specification<Incident>>any(), any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of(), PageRequest.of(1, 10), 25));

        ListIncidentsResult result = service.execute(query);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Specification<Incident>> specificationCaptor = ArgumentCaptor.forClass(Specification.class);
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(incidentRepository).findAll(specificationCaptor.capture(), pageableCaptor.capture());
        Pageable pageable = pageableCaptor.getValue();
        List<Sort.Order> orders = pageable.getSort().toList();
        assertThat(specificationCaptor.getValue()).isNotNull();
        assertThat(pageable.getPageNumber()).isEqualTo(1);
        assertThat(pageable.getPageSize()).isEqualTo(10);
        assertThat(orders).extracting(Sort.Order::getProperty)
                .containsExactly("createdAt", "id");
        assertThat(orders).extracting(Sort.Order::getDirection)
                .containsExactly(Sort.Direction.DESC, Sort.Direction.DESC);
        assertThat(result.totalElements()).isEqualTo(25);
        assertThat(result.totalPages()).isEqualTo(3);
        assertThat(result.hasNext()).isTrue();
        assertThat(result.hasPrevious()).isTrue();
    }

    @Test
    void mapsClassificationReporterAssigneeAndResponsibleTeam() {
        Incident incident = mock(Incident.class);
        User reporter = mock(User.class);
        User assignee = mock(User.class);
        Team team = mock(Team.class);
        Instant createdAt = Instant.parse("2026-08-01T10:15:30Z");
        Instant updatedAt = Instant.parse("2026-08-02T11:20:35Z");
        when(incidentRepository.findAll(
                ArgumentMatchers.<Specification<Incident>>any(), any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of(incident), PageRequest.of(0, 20), 1));
        when(incident.getId()).thenReturn(42L);
        when(incident.getTitle()).thenReturn("Database unavailable");
        when(incident.getCategory()).thenReturn(IncidentCategory.INFRASTRUCTURE);
        when(incident.getSource()).thenReturn(IncidentSource.AUTOMATIC);
        when(incident.getPriority()).thenReturn(IncidentPriority.CRITICAL);
        when(incident.getStatus()).thenReturn(IncidentStatus.IN_PROGRESS);
        when(incident.getReporter()).thenReturn(reporter);
        when(incident.getResponsibleTeam()).thenReturn(team);
        when(incident.getAssignee()).thenReturn(assignee);
        when(incident.getCreatedAt()).thenReturn(createdAt);
        when(incident.getUpdatedAt()).thenReturn(updatedAt);
        when(reporter.getId()).thenReturn(7L);
        when(reporter.getDisplayName()).thenReturn("Reporter");
        when(team.getId()).thenReturn(9L);
        when(team.getName()).thenReturn("Platform");
        when(team.getCode()).thenReturn("PLATFORM");
        when(assignee.getId()).thenReturn(8L);
        when(assignee.getDisplayName()).thenReturn("Assignee");

        ListIncidentItem item = service.execute(emptyQuery()).items().getFirst();

        assertThat(item.category()).isEqualTo(IncidentCategory.INFRASTRUCTURE);
        assertThat(item.source()).isEqualTo(IncidentSource.AUTOMATIC);
        assertThat(item.reporterId()).isEqualTo(7L);
        assertThat(item.reporterDisplayName()).isEqualTo("Reporter");
        assertThat(item.responsibleTeamId()).isEqualTo(9L);
        assertThat(item.responsibleTeamName()).isEqualTo("Platform");
        assertThat(item.responsibleTeamCode()).isEqualTo("PLATFORM");
        assertThat(item.assigneeId()).isEqualTo(8L);
        assertThat(item.assigneeDisplayName()).isEqualTo("Assignee");
        assertThat(item.createdAt()).isEqualTo(createdAt);
        assertThat(item.updatedAt()).isEqualTo(updatedAt);
    }

    @Test
    void mapsNullTeamAndAssigneeFields() {
        Incident incident = mock(Incident.class);
        User reporter = mock(User.class);
        when(incidentRepository.findAll(
                ArgumentMatchers.<Specification<Incident>>any(), any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of(incident), PageRequest.of(0, 20), 1));
        when(incident.getReporter()).thenReturn(reporter);

        ListIncidentItem item = service.execute(emptyQuery()).items().getFirst();

        assertThat(item.assigneeId()).isNull();
        assertThat(item.assigneeDisplayName()).isNull();
        assertThat(item.responsibleTeamId()).isNull();
        assertThat(item.responsibleTeamName()).isNull();
        assertThat(item.responsibleTeamCode()).isNull();
    }

    private static ListIncidentsQuery emptyQuery() {
        return new ListIncidentsQuery(0, 20, null, null, null, null, null);
    }
}
