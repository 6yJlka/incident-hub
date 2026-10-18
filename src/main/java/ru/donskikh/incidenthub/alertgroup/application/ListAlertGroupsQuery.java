package ru.donskikh.incidenthub.alertgroup.application;

import ru.donskikh.incidenthub.alertgroup.AlertGroupStatus;

public record ListAlertGroupsQuery(int page, int size, AlertGroupStatus status, Long rootServiceId) {
    public ListAlertGroupsQuery {
        if (page < 0 || size <= 0 || size > 100) {
            throw new IllegalArgumentException("page must be non-negative and size must be between 1 and 100");
        }
        if (rootServiceId != null && rootServiceId <= 0) {
            throw new IllegalArgumentException("rootServiceId must be positive");
        }
    }
}
