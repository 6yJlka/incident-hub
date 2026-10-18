package ru.donskikh.incidenthub.alertgroup.application;

public record GetAlertGroupQuery(long groupId) {
    public GetAlertGroupQuery {
        if (groupId <= 0) {
            throw new IllegalArgumentException("groupId must be positive");
        }
    }
}
