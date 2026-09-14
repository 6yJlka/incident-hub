package ru.donskikh.incidenthub.incident.web;

import org.springframework.stereotype.Component;
import ru.donskikh.incidenthub.incident.IncidentAction;
import ru.donskikh.incidenthub.incident.IncidentStatus;
import ru.donskikh.incidenthub.security.AuthenticatedUser;

import java.util.Arrays;
import java.util.List;

@Component
public class AvailableIncidentActions {

    private final IncidentActionAuthorization authorization;

    public AvailableIncidentActions(IncidentActionAuthorization authorization) {
        this.authorization = authorization;
    }

    public List<IncidentAction> forIncident(IncidentStatus status, AuthenticatedUser user) {
        return Arrays.stream(IncidentAction.values())
                .filter(action -> action.isAllowedFrom(status))
                .filter(action -> authorization.isAllowed(user, action))
                .toList();
    }
}
