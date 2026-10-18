package ru.donskikh.incidenthub.alert.web;

import com.fasterxml.jackson.annotation.JsonInclude;
import ru.donskikh.incidenthub.alert.application.AlertAcceptanceStatus;
import ru.donskikh.incidenthub.alert.application.AlertRejectionReason;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AlertAcceptanceResponse(
        String externalId,
        AlertAcceptanceStatus status,
        Long alertId,
        AlertRejectionReason reason
) {
}
