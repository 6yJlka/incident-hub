package ru.donskikh.incidenthub.alertgroup;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import ru.donskikh.incidenthub.alert.correlation.Evidence;
import ru.donskikh.incidenthub.alert.correlation.RootAssessment;
import ru.donskikh.incidenthub.alert.correlation.RootAssessmentStatus;
import ru.donskikh.incidenthub.alert.correlation.RootCandidate;
import ru.donskikh.incidenthub.catalog.BusinessService;
import ru.donskikh.incidenthub.catalog.ServiceTier;
import ru.donskikh.incidenthub.identity.User;
import ru.donskikh.incidenthub.incident.Incident;
import ru.donskikh.incidenthub.team.Team;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AlertGroupTest {

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.EnumSource(AlertGroupStatus.class)
    void enforcesTransitionsFromEveryStatus(AlertGroupStatus status) {
        var actor = new User("engineer@example.com", "Engineer");
        var incident = org.mockito.Mockito.mock(Incident.class);
        var confirmed = ru.donskikh.incidenthub.CorrelationTestData.group(service("ROOT"), Instant.now());
        var dismissed = ru.donskikh.incidenthub.CorrelationTestData.group(service("ROOT"), Instant.now());
        ReflectionTestUtils.setField(confirmed, "status", status);
        ReflectionTestUtils.setField(dismissed, "status", status);
        Instant decidedAt = Instant.now();
        if (status == AlertGroupStatus.OPEN) {
            confirmed.confirm(incident, actor, decidedAt);
            dismissed.dismiss(actor, decidedAt, "False positive");
            assertThat(confirmed.getStatus()).isEqualTo(AlertGroupStatus.CONFIRMED);
            assertThat(confirmed.getIncident()).isSameAs(incident);
            assertThat(confirmed.getDecidedBy()).isSameAs(actor);
            assertThat(confirmed.getDecidedAt()).isEqualTo(decidedAt);
            assertThat(dismissed.getStatus()).isEqualTo(AlertGroupStatus.DISMISSED);
            assertThat(dismissed.getIncident()).isNull();
            assertThat(dismissed.getDismissReason()).isEqualTo("False positive");
        } else {
            assertThatThrownBy(() -> confirmed.confirm(incident, actor, decidedAt))
                    .isInstanceOf(AlertGroupStateConflictException.class);
            assertThatThrownBy(() -> dismissed.dismiss(actor, decidedAt, null))
                    .isInstanceOf(AlertGroupStateConflictException.class);
        }
    }

    @Test
    void maintainsBoundsAndAlwaysUsesRootProvidedByCorrelator() {
        var firstRoot = service("FIRST");
        var chosenRoot = service("CHOSEN");
        Instant first = Instant.parse("2026-10-18T12:00:00Z");
        var group = ru.donskikh.incidenthub.CorrelationTestData.group(firstRoot, first);
        assertThat(group.getStatus()).isEqualTo(AlertGroupStatus.OPEN);
        assertThat(group.getIncident()).isNull();
        group.includeAlert(first.plusSeconds(600), chosenRoot, ru.donskikh.incidenthub.CorrelationTestData.rankedFor(chosenRoot));
        assertThat(group.getProbableRootService()).isSameAs(chosenRoot);
        assertThat(group.getFirstAlertAt()).isEqualTo(first);
        assertThat(group.getLastAlertAt()).isEqualTo(first.plusSeconds(600));
        group.includeAlert(first.minusSeconds(60), firstRoot, ru.donskikh.incidenthub.CorrelationTestData.rankedFor(firstRoot));
        assertThat(group.getFirstAlertAt()).isEqualTo(first.minusSeconds(60));
        assertThat(group.getLastAlertAt()).isEqualTo(first.plusSeconds(600));
        assertThat(group.getProbableRootService()).isSameAs(firstRoot);
    }

    @Test
    void doesNotAcceptAlertsIntoNonOpenGroups() {
        for (var status : new AlertGroupStatus[]{AlertGroupStatus.CONFIRMED, AlertGroupStatus.DISMISSED}) {
            var group = ru.donskikh.incidenthub.CorrelationTestData.group(service("FIRST"), Instant.now());
            ReflectionTestUtils.setField(group, "status", status);
            assertThatThrownBy(() -> group.includeAlert(Instant.now(), service("ROOT"), ru.donskikh.incidenthub.CorrelationTestData.rankedFor(service("ROOT"))))
                    .isInstanceOf(IllegalStateException.class);
        }
    }


    @Test
    void checksRootAndStatusConsistencyAndReplacesAssessmentCompletely() {
        var root = service("ROOT");
        ReflectionTestUtils.setField(root, "id", 42L);
        var ranked = new RootAssessment.Ranked(
                List.of(new RootCandidate(42, .8)),
                List.of(new Evidence.Migrated()));
        var unknown = new RootAssessment.Undetermined("INSUFFICIENT_EVIDENCE");
        assertThatThrownBy(() -> new AlertGroup(null, Instant.now(), ranked)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new AlertGroup(root, Instant.now(), unknown)).isInstanceOf(IllegalArgumentException.class);
        var group = new AlertGroup(root, Instant.now(), ranked);
        group.includeAlert(Instant.now(), null, unknown);
        assertThat(group.getRootAssessment()).isEqualTo(unknown);
        assertThat(group.getProbableRootService()).isNull();
        assertThat(group.getRootAssessmentStatus()).isEqualTo(RootAssessmentStatus.UNDETERMINED);
        assertThatThrownBy(() -> group.resolveIncidentService(null)).isInstanceOf(RootServiceRequiredException.class);
        assertThat(group.resolveIncidentService(root)).isSameAs(root);
        group.includeAlert(Instant.now(), root, ranked);
        assertThat(group.getRootAssessment()).isEqualTo(ranked);
        ReflectionTestUtils.setField(group, "rootAssessmentStatus", RootAssessmentStatus.UNDETERMINED);
        assertThatThrownBy(() -> ReflectionTestUtils.invokeMethod(group, "validateAssessment"))
                .isInstanceOf(IllegalArgumentException.class);
        ReflectionTestUtils.setField(group, "rootAssessmentStatus", RootAssessmentStatus.RANKED);
        ReflectionTestUtils.setField(root, "id", 43L);
        assertThatThrownBy(() -> ReflectionTestUtils.invokeMethod(group, "validateAssessment"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private BusinessService service(String code) {
        return new BusinessService(code, code, "Description", new Team("Team", "TEAM"), ServiceTier.TIER_1);
    }
}
