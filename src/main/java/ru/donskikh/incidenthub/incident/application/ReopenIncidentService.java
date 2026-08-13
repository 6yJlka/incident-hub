package ru.donskikh.incidenthub.incident.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.donskikh.incidenthub.audit.IncidentAuditEventType;
import ru.donskikh.incidenthub.audit.IncidentAuditService;
import ru.donskikh.incidenthub.incident.Incident;
import ru.donskikh.incidenthub.incident.IncidentNotFoundException;
import ru.donskikh.incidenthub.incident.IncidentRepository;
import ru.donskikh.incidenthub.incident.IncidentStatus;

import java.util.Objects;

@Service
public class ReopenIncidentService {

    private final IncidentRepository incidentRepository;
    private final IncidentAuditService auditService;

    public ReopenIncidentService(IncidentRepository incidentRepository, IncidentAuditService auditService) {
        this.incidentRepository = incidentRepository;
        this.auditService = auditService;
    }

    @Transactional
    public ReopenIncidentResult reopen(ReopenIncidentCommand command) {
        Objects.requireNonNull(command, "command must not be null");

        Incident incident = incidentRepository.findById(command.incidentId())
                .orElseThrow(() -> new IncidentNotFoundException(command.incidentId()));

        IncidentStatus fromStatus = incident.getStatus();
        incident.reopen();

        auditService.record(
                incident.getId(),
                IncidentAuditEventType.REOPENED,
                fromStatus,
                incident.getStatus()
        );

        return new ReopenIncidentResult(incident.getId(), incident.getStatus());
    }
}
