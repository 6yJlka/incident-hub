package ru.donskikh.incidenthub.incident.web;

import org.springframework.stereotype.Component;
import ru.donskikh.incidenthub.audit.application.GetIncidentHistoryResult;
import ru.donskikh.incidenthub.audit.application.IncidentHistoryItem;
import ru.donskikh.incidenthub.incident.IncidentPriority;
import ru.donskikh.incidenthub.incident.IncidentSeverity;
import ru.donskikh.incidenthub.incident.IncidentSource;
import ru.donskikh.incidenthub.incident.IncidentStatus;
import ru.donskikh.incidenthub.incident.application.AssignIncidentCommand;
import ru.donskikh.incidenthub.incident.application.CreateIncidentCommand;
import ru.donskikh.incidenthub.incident.application.CreateIncidentResult;
import ru.donskikh.incidenthub.incident.application.GetIncidentResult;
import ru.donskikh.incidenthub.incident.application.ListIncidentItem;
import ru.donskikh.incidenthub.incident.application.ListIncidentsQuery;
import ru.donskikh.incidenthub.incident.application.ListIncidentsResult;

@Component
public class IncidentWebMapper {

    public CreateIncidentCommand toCommand(CreateIncidentRequest request) {
        return new CreateIncidentCommand(
                request.title(),
                request.description(),
                request.affectedServiceId(),
                request.priority(),
                request.severity(),
                request.reporterId(),
                request.responsibleTeamId()
        );
    }

    public AssignIncidentCommand toCommand(long incidentId, AssignIncidentRequest request) {
        return new AssignIncidentCommand(incidentId, request.assigneeId());
    }

    public ListIncidentsQuery toQuery(
            int page,
            int size,
            IncidentStatus status,
            IncidentPriority priority,
            IncidentSeverity severity,
            IncidentSource source,
            Long affectedServiceId,
            Long responsibleTeamId
    ) {
        return new ListIncidentsQuery(
                page,
                size,
                status,
                priority,
                severity,
                source,
                affectedServiceId,
                responsibleTeamId
        );
    }

    public CreateIncidentResponse toResponse(CreateIncidentResult result) {
        return new CreateIncidentResponse(result.incidentId(), result.status());
    }

    public IncidentResponse toResponse(GetIncidentResult result) {
        return new IncidentResponse(
                result.id(),
                result.title(),
                result.description(),
                result.affectedServiceId(),
                result.affectedServiceCode(),
                result.affectedServiceName(),
                result.source(),
                result.priority(),
                result.severity(),
                result.status(),
                result.reporterId(),
                result.reporterDisplayName(),
                result.responsibleTeamId(),
                result.responsibleTeamName(),
                result.responsibleTeamCode(),
                result.assigneeId(),
                result.assigneeDisplayName(),
                result.createdAt(),
                result.updatedAt()
        );
    }

    public ListIncidentsResponse toResponse(ListIncidentsResult result) {
        return new ListIncidentsResponse(
                result.items().stream().map(this::toResponse).toList(),
                result.page(),
                result.size(),
                result.totalElements(),
                result.totalPages(),
                result.hasNext(),
                result.hasPrevious()
        );
    }

    public IncidentHistoryResponse toResponse(GetIncidentHistoryResult result) {
        return new IncidentHistoryResponse(
                result.incidentId(),
                result.items().stream().map(this::toResponse).toList()
        );
    }

    private IncidentListItemResponse toResponse(ListIncidentItem item) {
        return new IncidentListItemResponse(
                item.id(),
                item.title(),
                item.affectedServiceId(),
                item.affectedServiceCode(),
                item.affectedServiceName(),
                item.source(),
                item.priority(),
                item.severity(),
                item.status(),
                item.reporterId(),
                item.reporterDisplayName(),
                item.responsibleTeamId(),
                item.responsibleTeamName(),
                item.responsibleTeamCode(),
                item.assigneeId(),
                item.assigneeDisplayName(),
                item.createdAt(),
                item.updatedAt()
        );
    }

    private IncidentHistoryItemResponse toResponse(IncidentHistoryItem item) {
        return new IncidentHistoryItemResponse(
                item.id(),
                item.eventType(),
                item.fromStatus(),
                item.toStatus(),
                item.createdAt()
        );
    }
}
