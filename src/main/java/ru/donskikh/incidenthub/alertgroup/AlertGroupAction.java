package ru.donskikh.incidenthub.alertgroup;

public enum AlertGroupAction {
    CONFIRM, DISMISS;

    public boolean isAllowedFrom(AlertGroupStatus status) {
        return status == AlertGroupStatus.OPEN;
    }
}
