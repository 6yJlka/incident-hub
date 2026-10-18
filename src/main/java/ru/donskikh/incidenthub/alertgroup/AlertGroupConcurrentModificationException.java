package ru.donskikh.incidenthub.alertgroup;

import ru.donskikh.incidenthub.common.DomainConflictException;

public class AlertGroupConcurrentModificationException extends DomainConflictException {
    private final long groupId;

    public AlertGroupConcurrentModificationException(long groupId, Throwable cause) {
        super("Alert group " + groupId + " changed; refresh it before retrying", cause);
        this.groupId = groupId;
    }

    public long getGroupId() {
        return groupId;
    }
}
