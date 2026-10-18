package ru.donskikh.incidenthub.alertgroup.web;

import org.springframework.stereotype.Component;
import ru.donskikh.incidenthub.alertgroup.AlertGroupStatus;
import ru.donskikh.incidenthub.alertgroup.application.AlertGroupItem;
import ru.donskikh.incidenthub.alertgroup.application.ListAlertGroupsQuery;
import ru.donskikh.incidenthub.alertgroup.application.ListAlertGroupsResult;
import ru.donskikh.incidenthub.alertgroup.application.GetAlertGroupQuery;
import ru.donskikh.incidenthub.alertgroup.application.GetAlertGroupResult;
import ru.donskikh.incidenthub.alertgroup.application.GroupAlertItem;
import ru.donskikh.incidenthub.alertgroup.application.ConfirmAlertGroupCommand;
import ru.donskikh.incidenthub.alertgroup.application.DismissAlertGroupCommand;
import ru.donskikh.incidenthub.security.AuthenticatedUser;

@Component
public class AlertGroupWebMapper {

    private final AvailableAlertGroupActions availableActions;

    public AlertGroupWebMapper(AvailableAlertGroupActions availableActions) {
        this.availableActions = availableActions;
    }

    public ConfirmAlertGroupCommand toCommand(long id, ConfirmAlertGroupRequest request, long actorId) {
        return new ConfirmAlertGroupCommand(id, request.severity(), request.priority(), request.businessServiceId(),
                request.title(), request.description(), actorId);
    }

    public DismissAlertGroupCommand toCommand(long id, DismissAlertGroupRequest request, long actorId) {
        return new DismissAlertGroupCommand(id, request == null ? null : request.reason(), actorId);
    }

    public ListAlertGroupsQuery toQuery(int page, int size, AlertGroupStatus status, Long rootServiceId) {
        return new ListAlertGroupsQuery(page, size, status, rootServiceId);
    }

    public GetAlertGroupQuery toQuery(long id) {
        return new GetAlertGroupQuery(id);
    }

    public ListAlertGroupsResponse toResponse(ListAlertGroupsResult result) {
        return new ListAlertGroupsResponse(result.items().stream().map(this::toResponse).toList(),
                result.page(), result.size(), result.totalElements(), result.totalPages(),
                result.hasNext(), result.hasPrevious());
    }

    public AlertGroupDetailResponse toResponse(GetAlertGroupResult result, AuthenticatedUser user) {
        AlertGroupItem item = result.group();
        return new AlertGroupDetailResponse(
                item.id(),
                item.status(),
                item.probableRootServiceId(),
                item.probableRootServiceCode(),
                item.probableRootServiceName(),
                item.firstAlertAt(),
                item.lastAlertAt(),
                item.incidentId(),
                result.alerts().stream().map(this::toResponse).toList(),
                result.decidedById(), result.decidedByDisplayName(), result.decidedAt(), result.dismissReason(),
                availableActions.forGroup(item.status(), user));
    }

    private AlertGroupResponse toResponse(AlertGroupItem item) {
        return new AlertGroupResponse(
                item.id(),
                item.status(),
                item.probableRootServiceId(),
                item.probableRootServiceCode(),
                item.probableRootServiceName(),
                item.firstAlertAt(),
                item.lastAlertAt(),
                item.incidentId());
    }

    private GroupAlertResponse toResponse(GroupAlertItem item) {
        return new GroupAlertResponse(
                item.id(),
                item.source(),
                item.externalId(),
                item.businessServiceId(),
                item.businessServiceCode(),
                item.businessServiceName(),
                item.signalType(),
                item.severity(),
                item.occurredAt(),
                item.receivedAt(),
                item.description());
    }
}
