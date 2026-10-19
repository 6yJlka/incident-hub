package ru.donskikh.incidenthub.alertgroup.application;

import ru.donskikh.incidenthub.alertgroup.AlertGroup;
import ru.donskikh.incidenthub.alertgroup.AlertGroupStatus;

import java.time.Instant;
import java.util.Objects;
import java.util.List;
import ru.donskikh.incidenthub.alert.AlertSeverity;
import ru.donskikh.incidenthub.alertgroup.AlertGroupRepository;

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
        AlertSeverity worstSeverity
) {
    public AlertGroupItem {
        servicePreviewCodes = List.copyOf(servicePreviewCodes);
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(probableRootServiceId, "probableRootServiceId must not be null");
        Objects.requireNonNull(firstAlertAt, "firstAlertAt must not be null");
        Objects.requireNonNull(lastAlertAt, "lastAlertAt must not be null");
    }

    public AlertGroupItem(Long id, AlertGroupStatus status, Long probableRootServiceId,
            String probableRootServiceCode, String probableRootServiceName, Instant firstAlertAt,
            Instant lastAlertAt, Long incidentId) {
        this(id, status, probableRootServiceId, probableRootServiceCode, probableRootServiceName,
                firstAlertAt, lastAlertAt, incidentId, 0, 0, List.of(), null);
    }

    public AlertGroupItem withSummary(AlertGroupRepository.SummaryProjection summary) {
        return summary == null ? this : new AlertGroupItem(id, status, probableRootServiceId,
                probableRootServiceCode, probableRootServiceName, firstAlertAt, lastAlertAt, incidentId,
                summary.getAlertCount(), summary.getServiceCount(),
                List.of(summary.getServicePreviewCodes()), AlertSeverity.valueOf(summary.getWorstSeverity()));
    }

    public static AlertGroupItem from(AlertGroup group) {
        return new AlertGroupItem(group.getId(), group.getStatus(), group.getProbableRootService().getId(),
                group.getProbableRootService().getCode(), group.getProbableRootService().getName(),
                group.getFirstAlertAt(), group.getLastAlertAt(),
                group.getIncident() == null ? null : group.getIncident().getId());
    }
}
