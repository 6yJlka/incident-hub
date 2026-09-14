package ru.donskikh.incidenthub.audit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.donskikh.incidenthub.identity.User;
import ru.donskikh.incidenthub.identity.UserRepository;
import ru.donskikh.incidenthub.incident.IncidentStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IncidentAuditServiceTest {

    @Mock
    private IncidentAuditEventRepository repository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private IncidentAuditService service;

    @Test
    void recordsIncidentAuditEvent() {
        User actor = new User("operator@example.com", "Operator");
        when(userRepository.getReferenceById(7L)).thenReturn(actor);

        service.record(
                42L,
                IncidentAuditEventType.RESOLVED,
                IncidentStatus.IN_PROGRESS,
                IncidentStatus.RESOLVED,
                7L
        );

        ArgumentCaptor<IncidentAuditEvent> eventCaptor = ArgumentCaptor.forClass(IncidentAuditEvent.class);
        verify(repository).save(eventCaptor.capture());
        IncidentAuditEvent event = eventCaptor.getValue();
        assertThat(event.getIncidentId()).isEqualTo(42L);
        assertThat(event.getEventType()).isEqualTo(IncidentAuditEventType.RESOLVED);
        assertThat(event.getFromStatus()).isEqualTo(IncidentStatus.IN_PROGRESS);
        assertThat(event.getToStatus()).isEqualTo(IncidentStatus.RESOLVED);
        assertThat(event.getActor()).isSameAs(actor);
        verify(userRepository).getReferenceById(7L);
    }
}
