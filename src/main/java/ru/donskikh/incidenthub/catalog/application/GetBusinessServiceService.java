package ru.donskikh.incidenthub.catalog.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.donskikh.incidenthub.catalog.BusinessService;
import ru.donskikh.incidenthub.catalog.BusinessServiceNotFoundException;
import ru.donskikh.incidenthub.catalog.BusinessServiceRepository;
import ru.donskikh.incidenthub.catalog.ServiceDependency;
import ru.donskikh.incidenthub.catalog.ServiceDependencyRepository;
import ru.donskikh.incidenthub.team.Team;

import java.util.List;
import java.util.Objects;

@Service
public class GetBusinessServiceService {

    private final BusinessServiceRepository businessServiceRepository;
    private final ServiceDependencyRepository serviceDependencyRepository;

    public GetBusinessServiceService(
            BusinessServiceRepository businessServiceRepository,
            ServiceDependencyRepository serviceDependencyRepository
    ) {
        this.businessServiceRepository = businessServiceRepository;
        this.serviceDependencyRepository = serviceDependencyRepository;
    }

    @Transactional(readOnly = true)
    public GetBusinessServiceResult get(GetBusinessServiceQuery query) {
        Objects.requireNonNull(query, "query must not be null");

        BusinessService businessService = businessServiceRepository.findById(query.businessServiceId())
                .orElseThrow(() -> new BusinessServiceNotFoundException(query.businessServiceId()));
        Team ownerTeam = businessService.getOwnerTeam();
        List<DirectServiceDependencyItem> dependencies = serviceDependencyRepository
                .findAllByDependent_IdOrderByIdAsc(businessService.getId())
                .stream()
                .map(this::toDependencyItem)
                .toList();

        return new GetBusinessServiceResult(
                businessService.getId(),
                businessService.getCode(),
                businessService.getName(),
                businessService.getDescription(),
                ownerTeam.getId(),
                ownerTeam.getName(),
                ownerTeam.getCode(),
                businessService.getTier(),
                businessService.isActive(),
                businessService.getCreatedAt(),
                businessService.getUpdatedAt(),
                dependencies
        );
    }

    private DirectServiceDependencyItem toDependencyItem(ServiceDependency relationship) {
        BusinessService dependency = relationship.getDependency();

        return new DirectServiceDependencyItem(
                relationship.getId(),
                dependency.getId(),
                dependency.getCode(),
                dependency.getName(),
                dependency.getTier(),
                dependency.isActive(),
                relationship.getType(),
                relationship.getCreatedAt()
        );
    }
}
