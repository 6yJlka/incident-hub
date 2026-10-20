package ru.donskikh.incidenthub.alert.correlation;

import java.util.List;
import java.util.Objects;

public sealed interface CorrelationDecision {
    RootAssessment rootAssessment();
    List<Evidence> linkEvidence();

    record JoinGroup(long groupId, RootAssessment rootAssessment, LinkStrength linkStrength,
                     List<Evidence> linkEvidence) implements CorrelationDecision {
        public JoinGroup {
            if (groupId <= 0) throw new IllegalArgumentException("groupId must be positive");
            Objects.requireNonNull(rootAssessment, "rootAssessment must not be null");
            Objects.requireNonNull(linkStrength, "linkStrength must not be null");
            linkEvidence = Evidence.requireEvidence(linkEvidence);
        }
    }

    record OpenGroup(RootAssessment rootAssessment, List<Evidence> linkEvidence) implements CorrelationDecision {
        public OpenGroup {
            Objects.requireNonNull(rootAssessment, "rootAssessment must not be null");
            linkEvidence = Evidence.requireEvidence(linkEvidence);
        }
    }
}
