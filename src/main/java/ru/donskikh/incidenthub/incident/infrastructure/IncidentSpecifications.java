package ru.donskikh.incidenthub.incident.infrastructure;

import org.springframework.data.jpa.domain.Specification;
import ru.donskikh.incidenthub.incident.Incident;
import ru.donskikh.incidenthub.incident.IncidentPriority;
import ru.donskikh.incidenthub.incident.IncidentSeverity;
import ru.donskikh.incidenthub.incident.IncidentSource;
import ru.donskikh.incidenthub.incident.IncidentStatus;

public final class IncidentSpecifications {

    private IncidentSpecifications() {
    }

    public static Specification<Incident> withFilters(
            IncidentStatus status,
            IncidentPriority priority,
            IncidentSeverity severity,
            IncidentSource source,
            Long affectedServiceId,
            Long responsibleTeamId
    ) {
        Specification<Incident> specification = Specification.unrestricted();

        if (status != null) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.equal(root.get("status"), status));
        }
        if (priority != null) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.equal(root.get("priority"), priority));
        }
        if (severity != null) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.equal(root.get("severity"), severity));
        }
        if (source != null) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.equal(root.get("source"), source));
        }
        if (affectedServiceId != null) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.equal(root.get("affectedService").get("id"), affectedServiceId));
        }
        if (responsibleTeamId != null) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.equal(root.get("responsibleTeam").get("id"), responsibleTeamId));
        }

        return specification;
    }
}
