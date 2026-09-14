package ru.donskikh.incidenthub.incident.web;

import org.springframework.stereotype.Component;
import ru.donskikh.incidenthub.identity.UserRole;
import ru.donskikh.incidenthub.incident.IncidentAction;
import ru.donskikh.incidenthub.security.AuthenticatedUser;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

@Component("incidentActionAuthorization")
public class IncidentActionAuthorization {

    private static final Set<UserRole> LIFECYCLE_ROLES = Set.copyOf(EnumSet.of(
            UserRole.ENGINEER,
            UserRole.ADMIN
    ));
    private static final Map<IncidentAction, Set<UserRole>> ALLOWED_ROLES = Map.of(
            IncidentAction.ASSIGN, LIFECYCLE_ROLES,
            IncidentAction.START, LIFECYCLE_ROLES,
            IncidentAction.RESOLVE, LIFECYCLE_ROLES,
            IncidentAction.CLOSE, LIFECYCLE_ROLES,
            IncidentAction.REOPEN, LIFECYCLE_ROLES,
            IncidentAction.CANCEL, LIFECYCLE_ROLES
    );

    public boolean isAllowed(AuthenticatedUser user, IncidentAction action) {
        if (user == null || action == null) {
            return false;
        }
        Set<UserRole> roles = ALLOWED_ROLES.get(action);
        return roles != null && roles.contains(user.role());
    }
}
