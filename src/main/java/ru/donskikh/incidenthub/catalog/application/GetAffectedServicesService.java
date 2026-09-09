package ru.donskikh.incidenthub.catalog.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.donskikh.incidenthub.catalog.BusinessServiceNotFoundException;
import ru.donskikh.incidenthub.catalog.BusinessServiceRepository;
import ru.donskikh.incidenthub.catalog.DependencyType;
import ru.donskikh.incidenthub.catalog.ServiceDependencyRepository;
import ru.donskikh.incidenthub.catalog.ServiceTier;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Service
public class GetAffectedServicesService {

    private final BusinessServiceRepository businessServiceRepository;
    private final ServiceDependencyRepository serviceDependencyRepository;

    public GetAffectedServicesService(
            BusinessServiceRepository businessServiceRepository,
            ServiceDependencyRepository serviceDependencyRepository
    ) {
        this.businessServiceRepository = businessServiceRepository;
        this.serviceDependencyRepository = serviceDependencyRepository;
    }

    @Transactional(readOnly = true)
    public GetAffectedServicesResult get(GetAffectedServicesQuery query) {
        Objects.requireNonNull(query, "query must not be null");

        if (!businessServiceRepository.existsById(query.businessServiceId())) {
            throw new BusinessServiceNotFoundException(query.businessServiceId());
        }

        List<AffectedServiceItem> affectedServices = serviceDependencyRepository
                .findAffectedServices(query.businessServiceId(), query.maxDepth())
                .stream()
                .map(this::toItem)
                .sorted(Comparator.comparingInt(AffectedServiceItem::depth)
                        .thenComparing(AffectedServiceItem::code))
                .toList();

        return new GetAffectedServicesResult(
                affectedServices,
                query.businessServiceId(),
                query.maxDepth()
        );
    }

    private AffectedServiceItem toItem(ServiceDependencyRepository.AffectedServiceProjection projection) {
        return new AffectedServiceItem(
                projection.getId(),
                projection.getCode(),
                projection.getName(),
                ServiceTier.valueOf(projection.getTier()),
                projection.getActive(),
                projection.getOwnerTeamId(),
                projection.getOwnerTeamName(),
                projection.getDepth(),
                DependencyType.valueOf(projection.getDependencyType())
        );
    }
}
