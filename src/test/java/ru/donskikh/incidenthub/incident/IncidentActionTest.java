package ru.donskikh.incidenthub.incident;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class IncidentActionTest {

    @Test
    void delegatesEveryActionToItsIncidentStatusRule() {
        for (IncidentStatus status : IncidentStatus.values()) {
            assertThat(IncidentAction.ASSIGN.isAllowedFrom(status)).isEqualTo(status.allowsAssignment());
            assertThat(IncidentAction.START.isAllowedFrom(status)).isEqualTo(status.allowsStartProgress());
            assertThat(IncidentAction.RESOLVE.isAllowedFrom(status)).isEqualTo(status.allowsResolve());
            assertThat(IncidentAction.CLOSE.isAllowedFrom(status)).isEqualTo(status.allowsClose());
            assertThat(IncidentAction.REOPEN.isAllowedFrom(status)).isEqualTo(status.allowsReopen());
            assertThat(IncidentAction.CANCEL.isAllowedFrom(status)).isEqualTo(status.allowsCancellation());
        }
    }

    @Test
    void preservesReassignmentAndResolvedCancellationRules() {
        assertThat(IncidentAction.ASSIGN.isAllowedFrom(IncidentStatus.ASSIGNED)).isTrue();
        assertThat(IncidentAction.CANCEL.isAllowedFrom(IncidentStatus.RESOLVED)).isFalse();
    }
}
