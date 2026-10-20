package ru.donskikh.incidenthub.alertgroup.application;

import ru.donskikh.incidenthub.alert.correlation.RootAssessment;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record GetAlertGroupResult(AlertGroupItem group, List<GroupAlertItem> alerts,
        Long decidedById, String decidedByDisplayName, Instant decidedAt, String dismissReason, long version,
        RootAssessment rootAssessment, Map<Long, RootCandidateService> rootCandidateServices) {
    public GetAlertGroupResult {
        Objects.requireNonNull(group, "group must not be null");
        Objects.requireNonNull(rootAssessment, "rootAssessment must not be null");
        rootCandidateServices = Map.copyOf(rootCandidateServices);
        alerts = List.copyOf(Objects.requireNonNull(alerts, "alerts must not be null"));
    }

    public GetAlertGroupResult(AlertGroupItem group, List<GroupAlertItem> alerts,
            Long decidedById, String decidedByDisplayName, Instant decidedAt, String dismissReason,
            long version, RootAssessment rootAssessment) {
        this(group, alerts, decidedById, decidedByDisplayName, decidedAt, dismissReason,
                version, rootAssessment, Map.of());
    }
}
