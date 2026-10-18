package ru.donskikh.incidenthub.alertgroup.application;

public record CorrelationBatchResult(boolean lockAcquired, int processedAlerts, int createdGroups) {
    public CorrelationBatchResult {
        if (processedAlerts < 0 || createdGroups < 0 || createdGroups > processedAlerts) {
            throw new IllegalArgumentException("Invalid correlation result counts");
        }
        if (!lockAcquired && processedAlerts != 0) {
            throw new IllegalArgumentException("A skipped batch must not process alerts");
        }
    }
}
