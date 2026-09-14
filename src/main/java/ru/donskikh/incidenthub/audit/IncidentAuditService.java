package ru.donskikh.incidenthub.audit;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import ru.donskikh.incidenthub.incident.IncidentStatus;
import ru.donskikh.incidenthub.identity.User;
import ru.donskikh.incidenthub.identity.UserRepository;

@Service
public class IncidentAuditService {

    private final IncidentAuditEventRepository repository;
    private final UserRepository userRepository;

    public IncidentAuditService(IncidentAuditEventRepository repository, UserRepository userRepository) {
        this.repository = repository;
        this.userRepository = userRepository;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void record(
            long incidentId,
            IncidentAuditEventType eventType,
            IncidentStatus fromStatus,
            IncidentStatus toStatus,
            long actorId
    ) {
        if (actorId <= 0) {
            throw new IllegalArgumentException("actorId must be positive");
        }
        User actor = userRepository.getReferenceById(actorId);
        repository.save(new IncidentAuditEvent(incidentId, eventType, fromStatus, toStatus, actor));
    }
}
