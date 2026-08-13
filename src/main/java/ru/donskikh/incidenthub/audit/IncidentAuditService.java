package ru.donskikh.incidenthub.audit;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import ru.donskikh.incidenthub.incident.IncidentStatus;

@Service
public class IncidentAuditService {

    private final IncidentAuditEventRepository repository;

    public IncidentAuditService(IncidentAuditEventRepository repository) {
        this.repository = repository;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void record(
            long incidentId,
            IncidentAuditEventType eventType,
            IncidentStatus fromStatus,
            IncidentStatus toStatus
    ) {
        repository.save(new IncidentAuditEvent(incidentId, eventType, fromStatus, toStatus));
    }
}
