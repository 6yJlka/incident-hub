package ru.donskikh.incidenthub.alertgroup.web;

import ru.donskikh.incidenthub.alertgroup.AlertGroupStatus;

import java.time.Instant;
import java.util.List;
import ru.donskikh.incidenthub.alert.AlertSeverity;

public record AlertGroupResponse(
        Long id,
        AlertGroupStatus status,
        Long probableRootServiceId,
        String probableRootServiceCode,
        String probableRootServiceName,
        Instant firstAlertAt,
        Instant lastAlertAt,
        Long incidentId,
        long alertCount,
        long serviceCount,
        List<String> servicePreviewCodes,
        AlertSeverity worstSeverity
) {
    public AlertGroupResponse {
        servicePreviewCodes = List.copyOf(servicePreviewCodes);
    }
}
