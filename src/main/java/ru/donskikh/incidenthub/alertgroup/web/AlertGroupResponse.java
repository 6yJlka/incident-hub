package ru.donskikh.incidenthub.alertgroup.web;

import ru.donskikh.incidenthub.alertgroup.AlertGroupStatus;

import java.time.Instant;

public record AlertGroupResponse(
        Long id,
        AlertGroupStatus status,
        Long probableRootServiceId,
        String probableRootServiceCode,
        String probableRootServiceName,
        Instant firstAlertAt,
        Instant lastAlertAt,
        Long incidentId
) {
}
