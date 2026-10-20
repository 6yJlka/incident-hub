package ru.donskikh.incidenthub.alertgroup.web;

import ru.donskikh.incidenthub.alert.AlertSeverity;
import ru.donskikh.incidenthub.alert.AlertSignalType;
import ru.donskikh.incidenthub.alert.correlation.LinkStrength;

import java.time.Instant;
import java.util.List;

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
        String description,
        LinkStrength linkStrength,
        List<EvidenceResponse> linkEvidence
) {
    public GroupAlertResponse {
        linkEvidence = List.copyOf(linkEvidence);
    }
}
