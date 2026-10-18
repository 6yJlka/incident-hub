package ru.donskikh.incidenthub.alert.application;

import java.util.List;
import java.util.Objects;

public record AcceptAlertsResult(List<AlertAcceptanceResult> results) {
    public AcceptAlertsResult {
        results = List.copyOf(Objects.requireNonNull(results, "results must not be null"));
    }
}
