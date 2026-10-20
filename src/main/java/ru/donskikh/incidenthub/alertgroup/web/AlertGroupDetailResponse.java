package ru.donskikh.incidenthub.alertgroup.web;

import io.swagger.v3.oas.annotations.media.Schema;
import ru.donskikh.incidenthub.alertgroup.AlertGroupAction;
import ru.donskikh.incidenthub.alertgroup.AlertGroupStatus;

import java.time.Instant;
import java.util.List;

public record AlertGroupDetailResponse(
        Long id,
        long version,
        AlertGroupStatus status,
        @Schema(types = {"integer", "null"}, format = "int64") Long probableRootServiceId,
        @Schema(types = {"string", "null"}) String probableRootServiceCode,
        @Schema(types = {"string", "null"}) String probableRootServiceName,
        Instant firstAlertAt,
        Instant lastAlertAt,
        Long incidentId,
        List<GroupAlertResponse> alerts,
        Long decidedById,
        String decidedByDisplayName,
        Instant decidedAt,
        String dismissReason,
        List<AlertGroupAction> availableActions,
        RootAssessmentResponse rootAssessment,
        long tentativeLinkCount
) {
    public AlertGroupDetailResponse {
        alerts = List.copyOf(alerts);
        availableActions = List.copyOf(availableActions);
    }
}
