package ru.donskikh.incidenthub.incident.application;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.donskikh.incidenthub.identity.User;
import ru.donskikh.incidenthub.incident.Incident;
import ru.donskikh.incidenthub.incident.IncidentRepository;
import ru.donskikh.incidenthub.incident.infrastructure.IncidentSpecifications;
import ru.donskikh.incidenthub.team.Team;

import java.util.Objects;

@Service
@Transactional(readOnly = true)
public class ListIncidentsService {

    private final IncidentRepository incidentRepository;

    public ListIncidentsService(IncidentRepository incidentRepository) {
        this.incidentRepository = Objects.requireNonNull(
                incidentRepository,
                "incidentRepository must not be null"
        );
    }

    public ListIncidentsResult execute(ListIncidentsQuery query) {
        Objects.requireNonNull(query, "query must not be null");

        Specification<Incident> specification = IncidentSpecifications.withFilters(
                query.status(),
                query.priority(),
                query.category(),
                query.source(),
                query.responsibleTeamId()
        );
        Pageable pageable = PageRequest.of(
                query.page(),
                query.size(),
                Sort.by(
                        Sort.Order.desc("createdAt"),
                        Sort.Order.desc("id")
                )
        );
        Page<Incident> incidents = incidentRepository.findAll(specification, pageable);

        return new ListIncidentsResult(
                incidents.getContent().stream()
                        .map(this::toItem)
                        .toList(),
                incidents.getNumber(),
                incidents.getSize(),
                incidents.getTotalElements(),
                incidents.getTotalPages(),
                incidents.hasNext(),
                incidents.hasPrevious()
        );
    }

    private ListIncidentItem toItem(Incident incident) {
        User reporter = incident.getReporter();
        Team responsibleTeam = incident.getResponsibleTeam();
        User assignee = incident.getAssignee();

        return new ListIncidentItem(
                incident.getId(),
                incident.getTitle(),
                incident.getCategory(),
                incident.getSource(),
                incident.getPriority(),
                incident.getStatus(),
                reporter.getId(),
                reporter.getDisplayName(),
                responsibleTeam == null ? null : responsibleTeam.getId(),
                responsibleTeam == null ? null : responsibleTeam.getName(),
                responsibleTeam == null ? null : responsibleTeam.getCode(),
                assignee == null ? null : assignee.getId(),
                assignee == null ? null : assignee.getDisplayName(),
                incident.getCreatedAt(),
                incident.getUpdatedAt()
        );
    }
}
