package ru.donskikh.incidenthub.alertgroup.application;

import ru.donskikh.incidenthub.alertgroup.AlertGroup;
import ru.donskikh.incidenthub.alertgroup.AlertGroupStatus;

import java.time.Instant;
import java.util.Objects;

public record AlertGroupItem(
        Long id,
        AlertGroupStatus status,
        Long probableRootServiceId,
        String probableRootServiceCode,
        String probableRootServiceName,
        Instant firstAlertAt,
        Instant lastAlertAt,
        Long incidentId
) {
    public AlertGroupItem {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(probableRootServiceId, "probableRootServiceId must not be null");
        Objects.requireNonNull(firstAlertAt, "firstAlertAt must not be null");
        Objects.requireNonNull(lastAlertAt, "lastAlertAt must not be null");
    }

    public static AlertGroupItem from(AlertGroup group) {
        return new AlertGroupItem(group.getId(), group.getStatus(), group.getProbableRootService().getId(),
                group.getProbableRootService().getCode(), group.getProbableRootService().getName(),
                group.getFirstAlertAt(), group.getLastAlertAt(),
                group.getIncident() == null ? null : group.getIncident().getId());
    }
}
