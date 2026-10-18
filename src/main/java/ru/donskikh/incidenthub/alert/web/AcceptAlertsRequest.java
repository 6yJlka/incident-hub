package ru.donskikh.incidenthub.alert.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import ru.donskikh.incidenthub.alert.web.validation.ValidAlertBatchSize;

import java.util.List;

public record AcceptAlertsRequest(
        @NotEmpty @ValidAlertBatchSize List<@NotNull @Valid IncomingAlertRequest> alerts
) {
}
