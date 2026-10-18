package ru.donskikh.incidenthub.alert.correlation;

public sealed interface CorrelationDecision {

    long probableRootServiceId();

    record JoinGroup(long groupId, long probableRootServiceId) implements CorrelationDecision {
        public JoinGroup {
            if (groupId <= 0 || probableRootServiceId <= 0) {
                throw new IllegalArgumentException("Group and root service identifiers must be positive");
            }
        }
    }

    record OpenGroup(long probableRootServiceId) implements CorrelationDecision {
        public OpenGroup {
            if (probableRootServiceId <= 0) {
                throw new IllegalArgumentException("probableRootServiceId must be positive");
            }
        }
    }
}
