package ru.donskikh.incidenthub.audit.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.donskikh.incidenthub.audit.IncidentAuditEvent;
import ru.donskikh.incidenthub.audit.IncidentAuditEventRepository;
import ru.donskikh.incidenthub.incident.IncidentNotFoundException;
import ru.donskikh.incidenthub.incident.IncidentRepository;

@Service
public class GetIncidentHistoryService {

    private final IncidentRepository incidentRepository;
    private final IncidentAuditEventRepository auditEventRepository;

    public GetIncidentHistoryService(
            IncidentRepository incidentRepository,
            IncidentAuditEventRepository auditEventRepository
    ) {
        this.incidentRepository = incidentRepository;
        this.auditEventRepository = auditEventRepository;
    }

    @Transactional(readOnly = true)
    public GetIncidentHistoryResult get(long incidentId) {
        if (incidentId <= 0) {
            throw new IllegalArgumentException("incidentId must be positive");
        }

        incidentRepository.findById(incidentId)
                .orElseThrow(() -> new IncidentNotFoundException(incidentId));

        return new GetIncidentHistoryResult(
                incidentId,
                auditEventRepository.findByIncidentIdOrderByCreatedAtAscIdAsc(incidentId).stream()
                        .map(this::toItem)
                        .toList()
        );
    }

    private IncidentHistoryItem toItem(IncidentAuditEvent event) {
        return new IncidentHistoryItem(
                event.getId(),
                event.getEventType(),
                event.getFromStatus(),
                event.getToStatus(),
                event.getCreatedAt()
        );
    }
}
