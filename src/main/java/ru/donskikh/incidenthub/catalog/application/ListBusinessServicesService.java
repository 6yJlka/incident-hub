package ru.donskikh.incidenthub.catalog.application;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.donskikh.incidenthub.catalog.BusinessService;
import ru.donskikh.incidenthub.catalog.BusinessServiceRepository;
import ru.donskikh.incidenthub.catalog.infrastructure.BusinessServiceSpecifications;
import ru.donskikh.incidenthub.team.Team;

import java.util.Objects;

@Service
public class ListBusinessServicesService {

    private final BusinessServiceRepository businessServiceRepository;

    public ListBusinessServicesService(BusinessServiceRepository businessServiceRepository) {
        this.businessServiceRepository = businessServiceRepository;
    }

    @Transactional(readOnly = true)
    public ListBusinessServicesResult execute(ListBusinessServicesQuery query) {
        Objects.requireNonNull(query, "query must not be null");

        Specification<BusinessService> specification = BusinessServiceSpecifications.withFilters(
                query.ownerTeamId(),
                query.tier(),
                query.active()
        );
        Pageable pageable = PageRequest.of(
                query.page(),
                query.size(),
                Sort.by(
                        Sort.Order.asc("name"),
                        Sort.Order.asc("id")
                )
        );
        Page<BusinessService> businessServices = businessServiceRepository.findAll(specification, pageable);

        return new ListBusinessServicesResult(
                businessServices.getContent().stream()
                        .map(this::toItem)
                        .toList(),
                businessServices.getNumber(),
                businessServices.getSize(),
                businessServices.getTotalElements(),
                businessServices.getTotalPages(),
                businessServices.hasNext(),
                businessServices.hasPrevious()
        );
    }

    private ListBusinessServiceItem toItem(BusinessService businessService) {
        Team ownerTeam = businessService.getOwnerTeam();

        return new ListBusinessServiceItem(
                businessService.getId(),
                businessService.getCode(),
                businessService.getName(),
                ownerTeam.getId(),
                ownerTeam.getName(),
                ownerTeam.getCode(),
                businessService.getTier(),
                businessService.isActive(),
                businessService.getCreatedAt(),
                businessService.getUpdatedAt()
        );
    }
}
