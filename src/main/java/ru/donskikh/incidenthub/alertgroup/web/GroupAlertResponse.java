package ru.donskikh.incidenthub.alertgroup.web;

import ru.donskikh.incidenthub.alert.AlertSeverity;
import ru.donskikh.incidenthub.alert.AlertSignalType;

import java.time.Instant;

public record GroupAlertResponse(
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
}
