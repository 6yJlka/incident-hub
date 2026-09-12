package ru.donskikh.incidenthub.catalog.web;

import org.springframework.stereotype.Component;
import ru.donskikh.incidenthub.catalog.ServiceTier;
import ru.donskikh.incidenthub.catalog.application.AddServiceDependencyCommand;
import ru.donskikh.incidenthub.catalog.application.AddServiceDependencyResult;
import ru.donskikh.incidenthub.catalog.application.AffectedServiceItem;
import ru.donskikh.incidenthub.catalog.application.CreateBusinessServiceCommand;
import ru.donskikh.incidenthub.catalog.application.CreateBusinessServiceResult;
import ru.donskikh.incidenthub.catalog.application.DirectServiceDependencyItem;
import ru.donskikh.incidenthub.catalog.application.GetAffectedServicesQuery;
import ru.donskikh.incidenthub.catalog.application.GetAffectedServicesResult;
import ru.donskikh.incidenthub.catalog.application.GetBusinessServiceQuery;
import ru.donskikh.incidenthub.catalog.application.GetBusinessServiceResult;
import ru.donskikh.incidenthub.catalog.application.ListBusinessServiceItem;
import ru.donskikh.incidenthub.catalog.application.ListBusinessServicesQuery;
import ru.donskikh.incidenthub.catalog.application.ListBusinessServicesResult;
import ru.donskikh.incidenthub.catalog.application.RemoveServiceDependencyCommand;

@Component
public class BusinessServiceWebMapper {

    public CreateBusinessServiceCommand toCommand(CreateBusinessServiceRequest request) {
        return new CreateBusinessServiceCommand(
                request.code(),
                request.name(),
                request.description(),
                request.ownerTeamId(),
                request.tier()
        );
    }

    public AddServiceDependencyCommand toCommand(
            long dependentServiceId,
            AddServiceDependencyRequest request
    ) {
        return new AddServiceDependencyCommand(
                dependentServiceId,
                request.dependencyServiceId(),
                request.type()
        );
    }

    public RemoveServiceDependencyCommand toRemoveCommand(
            long dependentServiceId,
            long dependencyServiceId
    ) {
        return new RemoveServiceDependencyCommand(dependentServiceId, dependencyServiceId);
    }

    public ListBusinessServicesQuery toQuery(
            int page,
            int size,
            Long ownerTeamId,
            ServiceTier tier,
            Boolean active
    ) {
        return new ListBusinessServicesQuery(page, size, ownerTeamId, tier, active);
    }

    public GetBusinessServiceQuery toGetQuery(long businessServiceId) {
        return new GetBusinessServiceQuery(businessServiceId);
    }

    public GetAffectedServicesQuery toAffectedQuery(long businessServiceId, int maxDepth) {
        return new GetAffectedServicesQuery(businessServiceId, maxDepth);
    }

    public CreateBusinessServiceResponse toResponse(CreateBusinessServiceResult result) {
        return new CreateBusinessServiceResponse(
                result.businessServiceId(),
                result.code(),
                result.tier(),
                result.active()
        );
    }

    public AddServiceDependencyResponse toResponse(AddServiceDependencyResult result) {
        return new AddServiceDependencyResponse(
                result.relationshipId(),
                result.dependentServiceId(),
                result.dependencyServiceId(),
                result.type(),
                result.createdAt()
        );
    }

    public BusinessServiceResponse toResponse(GetBusinessServiceResult result) {
        return new BusinessServiceResponse(
                result.id(),
                result.code(),
                result.name(),
                result.description(),
                result.ownerTeamId(),
                result.ownerTeamName(),
                result.ownerTeamCode(),
                result.tier(),
                result.active(),
                result.createdAt(),
                result.updatedAt(),
                result.dependencies().stream().map(this::toResponse).toList()
        );
    }

    public ListBusinessServicesResponse toResponse(ListBusinessServicesResult result) {
        return new ListBusinessServicesResponse(
                result.items().stream().map(this::toResponse).toList(),
                result.page(),
                result.size(),
                result.totalElements(),
                result.totalPages(),
                result.hasNext(),
                result.hasPrevious()
        );
    }

    public GetAffectedServicesResponse toResponse(GetAffectedServicesResult result) {
        return new GetAffectedServicesResponse(
                result.items().stream().map(this::toResponse).toList(),
                result.businessServiceId(),
                result.maxDepth()
        );
    }

    private DirectServiceDependencyResponse toResponse(DirectServiceDependencyItem item) {
        return new DirectServiceDependencyResponse(
                item.relationshipId(),
                item.serviceId(),
                item.code(),
                item.name(),
                item.tier(),
                item.active(),
                item.type(),
                item.createdAt()
        );
    }

    private BusinessServiceListItemResponse toResponse(ListBusinessServiceItem item) {
        return new BusinessServiceListItemResponse(
                item.id(),
                item.code(),
                item.name(),
                item.ownerTeamId(),
                item.ownerTeamName(),
                item.ownerTeamCode(),
                item.tier(),
                item.active(),
                item.createdAt(),
                item.updatedAt()
        );
    }

    private AffectedServiceResponse toResponse(AffectedServiceItem item) {
        return new AffectedServiceResponse(
                item.id(),
                item.code(),
                item.name(),
                item.tier(),
                item.active(),
                item.ownerTeamId(),
                item.ownerTeamName(),
                item.depth(),
                item.dependencyType()
        );
    }
}
