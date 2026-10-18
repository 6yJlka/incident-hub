package ru.donskikh.incidenthub.alert.application;

import java.util.List;
import java.util.Objects;

public record AcceptAlertsCommand(List<IncomingAlert> alerts) {
    public AcceptAlertsCommand {
        alerts = List.copyOf(Objects.requireNonNull(alerts, "alerts must not be null"));
        if (alerts.isEmpty()) {
            throw new IllegalArgumentException("alerts must not be empty");
        }
    }
}
