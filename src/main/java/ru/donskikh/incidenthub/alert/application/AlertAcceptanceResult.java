package ru.donskikh.incidenthub.alert.application;

import java.util.Objects;

public record AlertAcceptanceResult(
        String externalId,
        AlertAcceptanceStatus status,
        Long alertId,
        AlertRejectionReason reason
) {
    public AlertAcceptanceResult {
        Objects.requireNonNull(externalId, "externalId must not be null");
        Objects.requireNonNull(status, "status must not be null");
        if (status == AlertAcceptanceStatus.REJECTED) {
            Objects.requireNonNull(reason, "rejection reason must not be null");
            if (alertId != null) {
                throw new IllegalArgumentException("rejected alert must not have an id");
            }
        } else if (alertId == null || alertId <= 0 || reason != null) {
            throw new IllegalArgumentException("accepted or duplicate alert must have an id and no rejection reason");
        }
    }
}
