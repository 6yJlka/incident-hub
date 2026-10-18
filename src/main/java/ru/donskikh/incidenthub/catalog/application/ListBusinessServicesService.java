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
import ru.donskikh.incidenthub.catalog.BusinessServiceRepository.ServiceCountsProjection;
import ru.donskikh.incidenthub.catalog.infrastructure.BusinessServiceSpecifications;
import ru.donskikh.incidenthub.team.Team;
import ru.donskikh.incidenthub.incident.IncidentStatus;

import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

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

        Map<Long, ServiceCountsProjection> counts = businessServices.isEmpty()
                ? Map.of()
                : businessServiceRepository.findCounts(
                        businessServices.getContent().stream().map(BusinessService::getId).toList(),
                        IncidentStatus.activeStatuses().stream().map(Enum::name).toList()
                ).stream().collect(Collectors.toMap(ServiceCountsProjection::getServiceId, Function.identity()));

        return new ListBusinessServicesResult(
                businessServices.getContent().stream()
                        .map(service -> toItem(service, counts.get(service.getId())))
                        .toList(),
                businessServices.getNumber(),
                businessServices.getSize(),
                businessServices.getTotalElements(),
                businessServices.getTotalPages(),
                businessServices.hasNext(),
                businessServices.hasPrevious()
        );
    }

    private ListBusinessServiceItem toItem(BusinessService businessService, ServiceCountsProjection counts) {
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
                businessService.getUpdatedAt(),
                counts == null ? 0 : counts.getDependencyCount(),
                counts == null ? 0 : counts.getDependentCount(),
                counts == null ? 0 : counts.getActiveIncidentCount()
        );
    }
}
