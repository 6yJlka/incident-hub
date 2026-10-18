package ru.donskikh.incidenthub.incident.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.donskikh.incidenthub.catalog.BusinessService;
import ru.donskikh.incidenthub.identity.User;
import ru.donskikh.incidenthub.incident.Incident;
import ru.donskikh.incidenthub.incident.IncidentNotFoundException;
import ru.donskikh.incidenthub.incident.IncidentRepository;
import ru.donskikh.incidenthub.team.Team;
import ru.donskikh.incidenthub.alertgroup.AlertGroupRepository;

import java.util.List;

@Service
public class GetIncidentService {

    private final IncidentRepository incidentRepository;
    private final AlertGroupRepository groupRepository;

    public GetIncidentService(IncidentRepository incidentRepository, AlertGroupRepository groupRepository) {
        this.incidentRepository = incidentRepository;
        this.groupRepository = groupRepository;
    }

    @Transactional(readOnly = true)
    public GetIncidentResult get(long incidentId) {
        if (incidentId <= 0) {
            throw new IllegalArgumentException("incidentId must be positive");
        }

        Incident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new IncidentNotFoundException(incidentId));

        BusinessService affectedService = incident.getAffectedService();
        User reporter = incident.getReporter();
        Team responsibleTeam = incident.getResponsibleTeam();
        User assignee = incident.getAssignee();

        return new GetIncidentResult(
                incident.getId(),
                incident.getTitle(),
                incident.getDescription(),
                affectedService.getId(),
                affectedService.getCode(),
                affectedService.getName(),
                incident.getSource(),
                incident.getPriority(),
                incident.getSeverity(),
                incident.getStatus(),
                reporter.getId(),
                reporter.getDisplayName(),
                responsibleTeam == null ? null : responsibleTeam.getId(),
                responsibleTeam == null ? null : responsibleTeam.getName(),
                responsibleTeam == null ? null : responsibleTeam.getCode(),
                assignee == null ? null : assignee.getId(),
                assignee == null ? null : assignee.getDisplayName(),
                incident.getCreatedAt(),
                incident.getUpdatedAt(),
                groupRepository.findIdByIncidentId(incidentId).orElse(null),
                List.of()
        );
    }
}
