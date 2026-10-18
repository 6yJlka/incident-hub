package ru.donskikh.incidenthub.alertgroup.application;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import ru.donskikh.incidenthub.alert.Alert;
import ru.donskikh.incidenthub.alert.AlertRepository;
import ru.donskikh.incidenthub.alert.correlation.AlertCorrelator;
import ru.donskikh.incidenthub.alert.correlation.CorrelationAlert;
import ru.donskikh.incidenthub.alert.correlation.CorrelationContext;
import ru.donskikh.incidenthub.alert.correlation.CorrelationDecision;
import ru.donskikh.incidenthub.alert.correlation.CorrelationGroupMember;
import ru.donskikh.incidenthub.alert.correlation.OpenAlertGroupSnapshot;
import ru.donskikh.incidenthub.alertgroup.AlertGroup;
import ru.donskikh.incidenthub.alertgroup.AlertGroupRepository;
import ru.donskikh.incidenthub.alertgroup.infrastructure.AlertCorrelationProperties;
import ru.donskikh.incidenthub.catalog.BusinessServiceRepository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class CorrelateAlertsService {

    private final AlertRepository alertRepository;
    private final AlertGroupRepository groupRepository;
    private final BusinessServiceRepository serviceRepository;
    private final AlertCorrelator correlator;
    private final AlertCorrelationProperties properties;

    public CorrelateAlertsService(
            AlertRepository alertRepository,
            AlertGroupRepository groupRepository,
            BusinessServiceRepository serviceRepository,
            AlertCorrelator correlator,
            AlertCorrelationProperties properties
    ) {
        this.alertRepository = alertRepository;
        this.groupRepository = groupRepository;
        this.serviceRepository = serviceRepository;
        this.correlator = correlator;
        this.properties = properties;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public CorrelationBatchResult process(CorrelateAlertsCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        if (!alertRepository.tryAcquireCorrelationLock()) {
            return new CorrelationBatchResult(false, 0, 0);
        }
        List<Alert> alerts = alertRepository.findAllByGroupIsNullOrderByOccurredAtAscIdAsc(
                PageRequest.of(0, command.batchSize())
        );
        if (alerts.isEmpty()) {
            return new CorrelationBatchResult(true, 0, 0);
        }
        Instant lowerBound = alerts.getFirst().getOccurredAt().minus(properties.window());
        Instant upperBound = alerts.getLast().getOccurredAt().plus(properties.window());
        Map<Long, AlertGroup> groups = new LinkedHashMap<>();
        groupRepository.findCorrelationCandidates(lowerBound, upperBound)
                .forEach(group -> groups.put(group.getId(), group));
        Map<Long, List<CorrelationGroupMember>> members = new HashMap<>();
        if (!groups.isEmpty()) {
            alertRepository.findCorrelationMembers(groups.keySet()).forEach(member ->
                    members.computeIfAbsent(member.getGroupId(), ignored -> new ArrayList<>()).add(
                            new CorrelationGroupMember(member.getBusinessServiceId(), member.getOccurredAt(),
                                    member.getSignalType(), member.getSeverity())
                    ));
        }
        Map<Long, OpenAlertGroupSnapshot> snapshots = new LinkedHashMap<>();
        groups.values().forEach(group -> snapshots.put(group.getId(),
                snapshot(group, members.getOrDefault(group.getId(), List.of()))));
        int createdGroups = 0;
        for (Alert alert : alerts) {
            CorrelationDecision decision = Objects.requireNonNull(correlator.correlate(
                    new CorrelationAlert(alert.getId(), alert.getBusinessService().getId(), alert.getOccurredAt(),
                            alert.getSignalType(), alert.getSeverity()),
                    new CorrelationContext(List.copyOf(snapshots.values()))
            ), "Correlation decision must not be null");
            var rootService = serviceRepository.getReferenceById(decision.probableRootServiceId());
            AlertGroup group;
            if (decision instanceof CorrelationDecision.OpenGroup) {
                group = groupRepository.save(new AlertGroup(rootService, alert.getOccurredAt()));
                groups.put(group.getId(), group);
                createdGroups++;
            } else {
                long groupId = ((CorrelationDecision.JoinGroup) decision).groupId();
                group = groups.get(groupId);
                if (group == null) {
                    throw new IllegalStateException("Correlator selected a group outside the context");
                }
                group.includeAlert(alert.getOccurredAt(), rootService);
            }
            if (alertRepository.attachToGroup(alert.getId(), group.getId()) != 1) {
                throw new IllegalStateException("Alert was already assigned by another processor");
            }
            var groupMembers = members.computeIfAbsent(group.getId(), ignored -> new ArrayList<>());
            groupMembers.add(new CorrelationGroupMember(alert.getBusinessService().getId(), alert.getOccurredAt(),
                    alert.getSignalType(), alert.getSeverity()));
            snapshots.put(group.getId(), snapshot(group, groupMembers));
        }
        return new CorrelationBatchResult(true, alerts.size(), createdGroups);
    }

    private OpenAlertGroupSnapshot snapshot(AlertGroup group, List<CorrelationGroupMember> members) {
        return new OpenAlertGroupSnapshot(group.getId(), group.getProbableRootService().getId(),
                group.getFirstAlertAt(), group.getLastAlertAt(), members);
    }
}
