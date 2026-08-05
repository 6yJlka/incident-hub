package ru.donskikh.incidenthub.incident.infrastructure;

import org.springframework.data.jpa.domain.Specification;
import ru.donskikh.incidenthub.incident.Incident;
import ru.donskikh.incidenthub.incident.IncidentPriority;
import ru.donskikh.incidenthub.incident.IncidentStatus;

public final class IncidentSpecifications {

    private IncidentSpecifications() {
    }

    public static Specification<Incident> withFilters(
            IncidentStatus status,
            IncidentPriority priority,
            String category
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
        if (category != null) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.equal(root.get("category"), category));
        }

        return specification;
    }
}
