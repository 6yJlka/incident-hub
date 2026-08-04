package ru.donskikh.incidenthub.incident.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.donskikh.incidenthub.identity.User;
import ru.donskikh.incidenthub.incident.Incident;
import ru.donskikh.incidenthub.incident.IncidentNotFoundException;
import ru.donskikh.incidenthub.incident.IncidentRepository;

@Service
public class GetIncidentService {

    private final IncidentRepository incidentRepository;

    public GetIncidentService(IncidentRepository incidentRepository) {
        this.incidentRepository = incidentRepository;
    }

    @Transactional(readOnly = true)
    public GetIncidentResult get(long incidentId) {
        if (incidentId <= 0) {
            throw new IllegalArgumentException("incidentId must be positive");
        }

        Incident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new IncidentNotFoundException(incidentId));

        User reporter = incident.getReporter();
        User assignee = incident.getAssignee();

        return new GetIncidentResult(
                incident.getId(),
                incident.getTitle(),
                incident.getDescription(),
                incident.getCategory(),
                incident.getPriority(),
                incident.getStatus(),
                reporter.getId(),
                reporter.getDisplayName(),
                assignee == null ? null : assignee.getId(),
                assignee == null ? null : assignee.getDisplayName(),
                incident.getCreatedAt(),
                incident.getUpdatedAt()
        );
    }
}
