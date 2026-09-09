package ru.donskikh.incidenthub.catalog.infrastructure;

import org.springframework.data.jpa.domain.Specification;
import ru.donskikh.incidenthub.catalog.BusinessService;
import ru.donskikh.incidenthub.catalog.ServiceTier;

public final class BusinessServiceSpecifications {

    private BusinessServiceSpecifications() {
    }

    public static Specification<BusinessService> withFilters(
            Long ownerTeamId,
            ServiceTier tier,
            Boolean active
    ) {
        Specification<BusinessService> specification = Specification.unrestricted();

        if (ownerTeamId != null) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.equal(root.get("ownerTeam").get("id"), ownerTeamId));
        }
        if (tier != null) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.equal(root.get("tier"), tier));
        }
        if (active != null) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.equal(root.get("active"), active));
        }

        return specification;
    }
}
