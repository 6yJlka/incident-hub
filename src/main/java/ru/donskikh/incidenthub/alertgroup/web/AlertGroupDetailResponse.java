package ru.donskikh.incidenthub.alertgroup.web;

import ru.donskikh.incidenthub.alertgroup.AlertGroupStatus;

import java.time.Instant;
import java.util.List;

public record AlertGroupDetailResponse(
        Long id,
        AlertGroupStatus status,
        Long probableRootServiceId,
        String probableRootServiceCode,
        String probableRootServiceName,
        Instant firstAlertAt,
        Instant lastAlertAt,
        Long incidentId,
        List<GroupAlertResponse> alerts
) {
    public AlertGroupDetailResponse {
        alerts = List.copyOf(alerts);
    }
}
