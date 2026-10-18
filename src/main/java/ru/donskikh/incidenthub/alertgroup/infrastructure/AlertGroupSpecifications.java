package ru.donskikh.incidenthub.alertgroup.infrastructure;

import org.springframework.data.jpa.domain.Specification;
import ru.donskikh.incidenthub.alertgroup.AlertGroup;
import ru.donskikh.incidenthub.alertgroup.AlertGroupStatus;

public final class AlertGroupSpecifications {

    private AlertGroupSpecifications() {
    }

    public static Specification<AlertGroup> withFilters(AlertGroupStatus status, Long rootServiceId) {
        Specification<AlertGroup> specification = Specification.unrestricted();
        if (status != null) {
            specification = specification.and((root, query, builder) -> builder.equal(root.get("status"), status));
        }
        if (rootServiceId != null) {
            specification = specification.and((root, query, builder) ->
                    builder.equal(root.get("probableRootService").get("id"), rootServiceId));
        }
        return specification;
    }
}
