package ru.donskikh.incidenthub.alertgroup.application;

import ru.donskikh.incidenthub.alert.AlertSeverity;
import ru.donskikh.incidenthub.alert.correlation.RootAssessmentStatus;
import ru.donskikh.incidenthub.alertgroup.AlertGroup;
import ru.donskikh.incidenthub.alertgroup.AlertGroupRepository;
import ru.donskikh.incidenthub.alertgroup.AlertGroupStatus;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record AlertGroupItem(
        Long id,
        AlertGroupStatus status,
        Long probableRootServiceId,
        String probableRootServiceCode,
        String probableRootServiceName,
        Instant firstAlertAt,
        Instant lastAlertAt,
        Long incidentId,
        long alertCount,
        long serviceCount,
        List<String> servicePreviewCodes,
        AlertSeverity worstSeverity,
        RootAssessmentStatus rootAssessmentStatus
) {
    public AlertGroupItem {
        servicePreviewCodes = List.copyOf(servicePreviewCodes);
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(firstAlertAt, "firstAlertAt must not be null");
        Objects.requireNonNull(lastAlertAt, "lastAlertAt must not be null");
    }

    public AlertGroupItem(Long id, AlertGroupStatus status, Long probableRootServiceId,
            String probableRootServiceCode, String probableRootServiceName, Instant firstAlertAt,
            Instant lastAlertAt, Long incidentId) {
        this(id, status, probableRootServiceId, probableRootServiceCode, probableRootServiceName,
                firstAlertAt, lastAlertAt, incidentId, 0, 0, List.of(), null,
                probableRootServiceId == null ? RootAssessmentStatus.UNDETERMINED
                        : RootAssessmentStatus.RANKED);
    }

    public AlertGroupItem withSummary(AlertGroupRepository.SummaryProjection summary) {
        return summary == null ? this : new AlertGroupItem(id, status, probableRootServiceId,
                probableRootServiceCode, probableRootServiceName, firstAlertAt, lastAlertAt, incidentId,
                summary.getAlertCount(), summary.getServiceCount(),
                List.of(summary.getServicePreviewCodes()), AlertSeverity.valueOf(summary.getWorstSeverity()), rootAssessmentStatus);
    }

    public static AlertGroupItem from(AlertGroup group) {
        var root = group.getProbableRootService();
        return new AlertGroupItem(group.getId(), group.getStatus(), root == null ? null : root.getId(),
                root == null ? null : root.getCode(),
                root == null ? null : root.getName(),
                group.getFirstAlertAt(), group.getLastAlertAt(),
                group.getIncident() == null ? null : group.getIncident().getId(),
                0, 0, List.of(), null, group.getRootAssessmentStatus());
    }
}
