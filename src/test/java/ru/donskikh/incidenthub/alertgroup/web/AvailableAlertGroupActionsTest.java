package ru.donskikh.incidenthub.alertgroup.web;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import ru.donskikh.incidenthub.alertgroup.AlertGroupAction;
import ru.donskikh.incidenthub.alertgroup.AlertGroupStatus;
import ru.donskikh.incidenthub.identity.UserRole;
import ru.donskikh.incidenthub.incident.web.IncidentActionAuthorization;
import ru.donskikh.incidenthub.security.AuthenticatedUser;
import static org.assertj.core.api.Assertions.assertThat;

class AvailableAlertGroupActionsTest {
    @ParameterizedTest
    @EnumSource(UserRole.class)
    void actionsMatchStatusAndRoleMatrix(UserRole role) {
        var matrix = new IncidentActionAuthorization();
        var actions = new AvailableAlertGroupActions(matrix);
        var user = new AuthenticatedUser(20L, "user@example.com", role);
        for (var status : AlertGroupStatus.values()) {
            var available = actions.forGroup(status, user);
            if (role != UserRole.REPORTER && status == AlertGroupStatus.OPEN) {
                assertThat(available).containsExactly(AlertGroupAction.CONFIRM, AlertGroupAction.DISMISS);
            } else {
                assertThat(available).isEmpty();
            }
            for (var action : AlertGroupAction.values()) {
                assertThat(available.contains(action)).isEqualTo(
                        matrix.isAllowed(user, action) && action.isAllowedFrom(status));
            }
        }
        assertThat(actions.forGroup(AlertGroupStatus.OPEN, null)).isEmpty();
    }
}
