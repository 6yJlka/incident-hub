package ru.donskikh.incidenthub.alertgroup;

import ru.donskikh.incidenthub.common.DomainConflictException;

public class AlertGroupStateConflictException extends DomainConflictException {
    public AlertGroupStateConflictException(AlertGroupStatus status) {
        super("Only open alert groups allow this action", status, AlertGroupStatus.OPEN);
    }
}
