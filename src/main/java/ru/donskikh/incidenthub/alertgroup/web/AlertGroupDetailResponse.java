package ru.donskikh.incidenthub.alertgroup.web;

import ru.donskikh.incidenthub.alertgroup.AlertGroupStatus;
import ru.donskikh.incidenthub.alertgroup.AlertGroupAction;

import java.time.Instant;
import java.util.List;

public record AlertGroupDetailResponse(
        Long id,
        long version,
        AlertGroupStatus status,
        Long probableRootServiceId,
        String probableRootServiceCode,
        String probableRootServiceName,
        Instant firstAlertAt,
        Instant lastAlertAt,
        Long incidentId,
        List<GroupAlertResponse> alerts,
        Long decidedById,
        String decidedByDisplayName,
        Instant decidedAt,
        String dismissReason,
        List<AlertGroupAction> availableActions
) {
    public AlertGroupDetailResponse {
        alerts = List.copyOf(alerts);
        availableActions = List.copyOf(availableActions);
    }
}
