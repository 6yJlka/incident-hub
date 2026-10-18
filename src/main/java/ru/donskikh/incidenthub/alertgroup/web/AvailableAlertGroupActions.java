package ru.donskikh.incidenthub.alertgroup.web;

import org.springframework.stereotype.Component;
import ru.donskikh.incidenthub.alertgroup.AlertGroupAction;
import ru.donskikh.incidenthub.alertgroup.AlertGroupStatus;
import ru.donskikh.incidenthub.incident.web.IncidentActionAuthorization;
import ru.donskikh.incidenthub.security.AuthenticatedUser;
import java.util.Arrays;
import java.util.List;

@Component
public class AvailableAlertGroupActions {
    private final IncidentActionAuthorization authorization;

    public AvailableAlertGroupActions(IncidentActionAuthorization authorization) {
        this.authorization = authorization;
    }

    public List<AlertGroupAction> forGroup(AlertGroupStatus status, AuthenticatedUser user) {
        return Arrays.stream(AlertGroupAction.values())
                .filter(action -> action.isAllowedFrom(status))
                .filter(action -> authorization.isAllowed(user, action)).toList();
    }
}
