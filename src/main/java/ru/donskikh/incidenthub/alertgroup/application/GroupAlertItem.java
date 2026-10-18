package ru.donskikh.incidenthub.alertgroup.application;

import ru.donskikh.incidenthub.alert.Alert;
import ru.donskikh.incidenthub.alert.AlertSeverity;
import ru.donskikh.incidenthub.alert.AlertSignalType;

import java.time.Instant;
import java.util.Objects;

public record GroupAlertItem(
        Long id,
        String source,
        String externalId,
        Long businessServiceId,
        String businessServiceCode,
        String businessServiceName,
        AlertSignalType signalType,
        AlertSeverity severity,
        Instant occurredAt,
        Instant receivedAt,
        String description
) {
    public GroupAlertItem {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(businessServiceId, "businessServiceId must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
    }

    public static GroupAlertItem from(Alert alert) {
        return new GroupAlertItem(alert.getId(), alert.getSource(), alert.getExternalId(),
                alert.getBusinessService().getId(), alert.getBusinessService().getCode(),
                alert.getBusinessService().getName(), alert.getSignalType(), alert.getSeverity(),
                alert.getOccurredAt(), alert.getReceivedAt(), alert.getDescription());
    }
}
