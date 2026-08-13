package ru.donskikh.incidenthub.incident.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.donskikh.incidenthub.audit.IncidentAuditEventType;
import ru.donskikh.incidenthub.audit.IncidentAuditService;
import ru.donskikh.incidenthub.identity.User;
import ru.donskikh.incidenthub.identity.UserNotFoundException;
import ru.donskikh.incidenthub.identity.UserRepository;
import ru.donskikh.incidenthub.incident.Incident;
import ru.donskikh.incidenthub.incident.IncidentNotFoundException;
import ru.donskikh.incidenthub.incident.IncidentRepository;
import ru.donskikh.incidenthub.incident.IncidentStatus;

import java.util.Objects;

@Service
public class AssignIncidentService {

    private final IncidentRepository incidentRepository;
    private final UserRepository userRepository;
    private final IncidentAuditService auditService;

    public AssignIncidentService(
            IncidentRepository incidentRepository,
            UserRepository userRepository,
            IncidentAuditService auditService
    ) {
        this.incidentRepository = incidentRepository;
        this.userRepository = userRepository;
        this.auditService = auditService;
    }

    @Transactional
    public AssignIncidentResult assign(AssignIncidentCommand command) {
        Objects.requireNonNull(command, "command must not be null");

        Incident incident = incidentRepository.findById(command.incidentId())
                .orElseThrow(() -> new IncidentNotFoundException(command.incidentId()));

        User assignee = userRepository.findById(command.assigneeId())
                .orElseThrow(() -> new UserNotFoundException(command.assigneeId()));

        IncidentStatus fromStatus = incident.getStatus();
        incident.assignTo(assignee);

        auditService.record(
                incident.getId(),
                IncidentAuditEventType.ASSIGNED,
                fromStatus,
                incident.getStatus()
        );

        return new AssignIncidentResult(
                incident.getId(),
                assignee.getId(),
                incident.getStatus()
        );
    }
}
