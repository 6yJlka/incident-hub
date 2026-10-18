package ru.donskikh.incidenthub.alertgroup;

import ru.donskikh.incidenthub.common.DomainNotFoundException;

public class AlertGroupNotFoundException extends DomainNotFoundException {

    public AlertGroupNotFoundException(long id) {
        super("Alert group %d was not found".formatted(id));
    }
}
