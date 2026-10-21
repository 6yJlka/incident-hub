package ru.donskikh.incidenthub.experiment.replay;

import ru.donskikh.incidenthub.alert.correlation.*;
import java.util.List;
import java.util.Objects;

public record ReplayMember(String externalKey, CorrelationAlert alert, LinkStrength linkStrength, List<Evidence> linkEvidence) {
    public ReplayMember {
        if (externalKey == null || externalKey.isBlank()) throw new IllegalArgumentException("External key is required");
        Objects.requireNonNull(alert, "alert must not be null");
        linkEvidence = Evidence.requireEvidence(linkEvidence);
    }
}
