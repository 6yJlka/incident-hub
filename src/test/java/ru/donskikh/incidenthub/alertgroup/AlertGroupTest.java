package ru.donskikh.incidenthub.alertgroup;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import ru.donskikh.incidenthub.catalog.BusinessService;
import ru.donskikh.incidenthub.catalog.ServiceTier;
import ru.donskikh.incidenthub.team.Team;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AlertGroupTest {

    @Test
    void maintainsBoundsAndAlwaysUsesRootProvidedByCorrelator() {
        var firstRoot = service("FIRST");
        var chosenRoot = service("CHOSEN");
        Instant first = Instant.parse("2026-10-18T12:00:00Z");
        var group = new AlertGroup(firstRoot, first);
        assertThat(group.getStatus()).isEqualTo(AlertGroupStatus.OPEN);
        assertThat(group.getIncident()).isNull();
        group.includeAlert(first.plusSeconds(600), chosenRoot);
        assertThat(group.getProbableRootService()).isSameAs(chosenRoot);
        assertThat(group.getFirstAlertAt()).isEqualTo(first);
        assertThat(group.getLastAlertAt()).isEqualTo(first.plusSeconds(600));
        group.includeAlert(first.minusSeconds(60), firstRoot);
        assertThat(group.getFirstAlertAt()).isEqualTo(first.minusSeconds(60));
        assertThat(group.getLastAlertAt()).isEqualTo(first.plusSeconds(600));
        assertThat(group.getProbableRootService()).isSameAs(firstRoot);
    }

    @Test
    void doesNotAcceptAlertsIntoNonOpenGroups() {
        for (var status : new AlertGroupStatus[]{AlertGroupStatus.CONFIRMED, AlertGroupStatus.DISMISSED}) {
            var group = new AlertGroup(service("FIRST"), Instant.now());
            ReflectionTestUtils.setField(group, "status", status);
            assertThatThrownBy(() -> group.includeAlert(Instant.now(), service("ROOT")))
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    private BusinessService service(String code) {
        return new BusinessService(code, code, "Description", new Team("Team", "TEAM"), ServiceTier.TIER_1);
    }
}
