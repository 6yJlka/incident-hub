package ru.donskikh.incidenthub.audit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.donskikh.incidenthub.incident.IncidentStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class IncidentAuditServiceTest {

    @Mock
    private IncidentAuditEventRepository repository;

    @InjectMocks
    private IncidentAuditService service;

    @Test
    void recordsIncidentAuditEvent() {
        service.record(
                42L,
                IncidentAuditEventType.RESOLVED,
                IncidentStatus.IN_PROGRESS,
                IncidentStatus.RESOLVED
        );

        ArgumentCaptor<IncidentAuditEvent> eventCaptor = ArgumentCaptor.forClass(IncidentAuditEvent.class);
        verify(repository).save(eventCaptor.capture());
        IncidentAuditEvent event = eventCaptor.getValue();
        assertThat(event.getIncidentId()).isEqualTo(42L);
        assertThat(event.getEventType()).isEqualTo(IncidentAuditEventType.RESOLVED);
        assertThat(event.getFromStatus()).isEqualTo(IncidentStatus.IN_PROGRESS);
        assertThat(event.getToStatus()).isEqualTo(IncidentStatus.RESOLVED);
    }
}
