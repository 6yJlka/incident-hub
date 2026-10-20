package ru.donskikh.incidenthub.alertgroup.web;

import io.swagger.v3.oas.annotations.media.Schema;
import ru.donskikh.incidenthub.alert.AlertSeverity;
import ru.donskikh.incidenthub.alert.correlation.RootAssessmentStatus;
import ru.donskikh.incidenthub.alertgroup.AlertGroupStatus;

import java.time.Instant;
import java.util.List;

public record AlertGroupResponse(
        Long id,
        AlertGroupStatus status,
        @Schema(types = {"integer", "null"}, format = "int64") Long probableRootServiceId,
        @Schema(types = {"string", "null"}) String probableRootServiceCode,
        @Schema(types = {"string", "null"}) String probableRootServiceName,
        Instant firstAlertAt,
        Instant lastAlertAt,
        Long incidentId,
        long alertCount,
        long serviceCount,
        List<String> servicePreviewCodes,
        AlertSeverity worstSeverity,
        RootAssessmentStatus rootAssessmentStatus
) {
    public AlertGroupResponse {
        servicePreviewCodes = List.copyOf(servicePreviewCodes);
    }
}
