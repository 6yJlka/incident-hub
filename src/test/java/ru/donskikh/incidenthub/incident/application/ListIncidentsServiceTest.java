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
import ru.donskikh.incidenthub.incident.IncidentPriority;
import ru.donskikh.incidenthub.incident.IncidentRepository;
import ru.donskikh.incidenthub.incident.IncidentStatus;

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
    void passesSpecificationAndStablePageRequestAndReturnsPageMetadata() {
        ListIncidentsQuery query = new ListIncidentsQuery(
                1,
                10,
                IncidentStatus.OPEN,
                IncidentPriority.HIGH,
                "Hardware"
        );
        when(incidentRepository.findAll(
                ArgumentMatchers.<Specification<Incident>>any(),
                any(Pageable.class)
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
        assertThat(orders).hasSize(2);
        assertThat(orders.get(0).getProperty()).isEqualTo("createdAt");
        assertThat(orders.get(0).getDirection()).isEqualTo(Sort.Direction.DESC);
        assertThat(orders.get(1).getProperty()).isEqualTo("id");
        assertThat(orders.get(1).getDirection()).isEqualTo(Sort.Direction.DESC);
        assertThat(result.items()).isEmpty();
        assertThat(result.page()).isEqualTo(1);
        assertThat(result.size()).isEqualTo(10);
        assertThat(result.totalElements()).isEqualTo(25);
        assertThat(result.totalPages()).isEqualTo(3);
        assertThat(result.hasNext()).isTrue();
        assertThat(result.hasPrevious()).isTrue();
    }

    @Test
    void mapsIncidentReporterAndExistingAssignee() {
        Incident incident = mock(Incident.class);
        User reporter = mock(User.class);
        User assignee = mock(User.class);
        Instant createdAt = Instant.parse("2026-08-01T10:15:30Z");
        Instant updatedAt = Instant.parse("2026-08-02T11:20:35Z");
        when(incidentRepository.findAll(
                ArgumentMatchers.<Specification<Incident>>any(),
                any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of(incident), PageRequest.of(0, 20), 1));
        when(incident.getId()).thenReturn(42L);
        when(incident.getTitle()).thenReturn("Database unavailable");
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

        ListIncidentItem item = service.execute(
                new ListIncidentsQuery(0, 20, null, null, null)
        ).items().getFirst();

        assertThat(item.id()).isEqualTo(42L);
        assertThat(item.title()).isEqualTo("Database unavailable");
        assertThat(item.category()).isEqualTo("Infrastructure");
        assertThat(item.priority()).isEqualTo(IncidentPriority.CRITICAL);
        assertThat(item.status()).isEqualTo(IncidentStatus.IN_PROGRESS);
        assertThat(item.reporterId()).isEqualTo(7L);
        assertThat(item.reporterDisplayName()).isEqualTo("Reporter");
        assertThat(item.assigneeId()).isEqualTo(8L);
        assertThat(item.assigneeDisplayName()).isEqualTo("Assignee");
        assertThat(item.createdAt()).isEqualTo(createdAt);
        assertThat(item.updatedAt()).isEqualTo(updatedAt);
    }

    @Test
    void mapsNullAssigneeFieldsForUnassignedIncident() {
        Incident incident = mock(Incident.class);
        User reporter = mock(User.class);
        when(incidentRepository.findAll(
                ArgumentMatchers.<Specification<Incident>>any(),
                any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of(incident), PageRequest.of(0, 20), 1));
        when(incident.getReporter()).thenReturn(reporter);

        ListIncidentItem item = service.execute(
                new ListIncidentsQuery(0, 20, null, null, null)
        ).items().getFirst();

        assertThat(item.assigneeId()).isNull();
        assertThat(item.assigneeDisplayName()).isNull();
    }
}
