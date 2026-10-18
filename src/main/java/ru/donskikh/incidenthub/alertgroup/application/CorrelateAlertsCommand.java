package ru.donskikh.incidenthub.alertgroup.application;

public record CorrelateAlertsCommand(int batchSize) {
    public CorrelateAlertsCommand {
        if (batchSize <= 0) {
            throw new IllegalArgumentException("batchSize must be positive");
        }
    }
}
