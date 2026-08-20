package ru.donskikh.incidenthub.audit.application;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.donskikh.incidenthub.audit.IncidentAuditEvent;
import ru.donskikh.incidenthub.audit.IncidentAuditEventRepository;
import ru.donskikh.incidenthub.audit.IncidentAuditEventType;
import ru.donskikh.incidenthub.incident.Incident;
import ru.donskikh.incidenthub.incident.IncidentNotFoundException;
import ru.donskikh.incidenthub.incident.IncidentRepository;
import ru.donskikh.incidenthub.incident.IncidentStatus;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetIncidentHistoryServiceTest {

    @Mock
    private IncidentRepository incidentRepository;

    @Mock
    private IncidentAuditEventRepository auditEventRepository;

    @InjectMocks
    private GetIncidentHistoryService service;

    @Test
    void rejectsNonPositiveIncidentId() {
        assertThatThrownBy(() -> service.get(0L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("incidentId must be positive");
        assertThatThrownBy(() -> service.get(-1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("incidentId must be positive");

        verifyNoInteractions(incidentRepository, auditEventRepository);
    }

    @Test
    void throwsWhenIncidentDoesNotExistWithoutLoadingAuditEvents() {
        when(incidentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(99L))
                .isInstanceOf(IncidentNotFoundException.class)
                .hasMessage("Incident not found: 99");

        verify(incidentRepository).findById(99L);
        verifyNoInteractions(auditEventRepository);
    }

    @Test
    void returnsEmptyHistoryForIncidentWithoutAuditEvents() {
        when(incidentRepository.findById(42L)).thenReturn(Optional.of(mock(Incident.class)));
        when(auditEventRepository.findByIncidentIdOrderByCreatedAtAscIdAsc(42L))
                .thenReturn(List.of());

        GetIncidentHistoryResult result = service.get(42L);

        assertThat(result.incidentId()).isEqualTo(42L);
        assertThat(result.items()).isEmpty();
    }

    @Test
    void mapsAuditEventsAndPreservesRepositoryOrder() {
        Instant createdAt = Instant.parse("2026-08-13T10:00:00Z");
        Instant resolvedAt = Instant.parse("2026-08-13T11:30:00Z");
        IncidentAuditEvent created = auditEvent(
                101L,
                IncidentAuditEventType.CREATED,
                null,
                IncidentStatus.OPEN,
                createdAt
        );
        IncidentAuditEvent resolved = auditEvent(
                102L,
                IncidentAuditEventType.RESOLVED,
                IncidentStatus.IN_PROGRESS,
                IncidentStatus.RESOLVED,
                resolvedAt
        );
        when(incidentRepository.findById(42L)).thenReturn(Optional.of(mock(Incident.class)));
        when(auditEventRepository.findByIncidentIdOrderByCreatedAtAscIdAsc(42L))
                .thenReturn(List.of(created, resolved));

        GetIncidentHistoryResult result = service.get(42L);

        assertThat(result.items()).containsExactly(
                new IncidentHistoryItem(
                        101L,
                        IncidentAuditEventType.CREATED,
                        null,
                        IncidentStatus.OPEN,
                        createdAt
                ),
                new IncidentHistoryItem(
                        102L,
                        IncidentAuditEventType.RESOLVED,
                        IncidentStatus.IN_PROGRESS,
                        IncidentStatus.RESOLVED,
                        resolvedAt
                )
        );
    }

    private IncidentAuditEvent auditEvent(
            long id,
            IncidentAuditEventType eventType,
            IncidentStatus fromStatus,
            IncidentStatus toStatus,
            Instant createdAt
    ) {
        IncidentAuditEvent event = mock(IncidentAuditEvent.class);
        when(event.getId()).thenReturn(id);
        when(event.getEventType()).thenReturn(eventType);
        when(event.getFromStatus()).thenReturn(fromStatus);
        when(event.getToStatus()).thenReturn(toStatus);
        when(event.getCreatedAt()).thenReturn(createdAt);
        return event;
    }
}
