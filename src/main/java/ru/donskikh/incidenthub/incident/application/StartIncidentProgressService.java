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
public class StartIncidentProgressService {

    private final IncidentRepository incidentRepository;
    private final IncidentAuditService auditService;

    public StartIncidentProgressService(IncidentRepository incidentRepository, IncidentAuditService auditService) {
        this.incidentRepository = incidentRepository;
        this.auditService = auditService;
    }

    @Transactional
    public StartIncidentProgressResult start(StartIncidentProgressCommand command) {
        Objects.requireNonNull(command, "command must not be null");

        Incident incident = incidentRepository.findById(command.incidentId())
                .orElseThrow(() -> new IncidentNotFoundException(command.incidentId()));

        IncidentStatus fromStatus = incident.getStatus();
        incident.startProgress();

        auditService.record(
                incident.getId(),
                IncidentAuditEventType.STARTED,
                fromStatus,
                incident.getStatus()
        );

        return new StartIncidentProgressResult(incident.getId(), incident.getStatus());
    }
}
