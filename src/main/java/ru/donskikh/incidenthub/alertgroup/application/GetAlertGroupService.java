package ru.donskikh.incidenthub.alertgroup.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.donskikh.incidenthub.alert.AlertRepository;
import ru.donskikh.incidenthub.alert.correlation.RootAssessment;
import ru.donskikh.incidenthub.alertgroup.AlertGroupNotFoundException;
import ru.donskikh.incidenthub.alertgroup.AlertGroupRepository;
import ru.donskikh.incidenthub.catalog.BusinessServiceRepository;

import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class GetAlertGroupService {

    private final AlertGroupRepository groupRepository;
    private final AlertRepository alertRepository;
    private final BusinessServiceRepository serviceRepository;

    public GetAlertGroupService(AlertGroupRepository groupRepository, AlertRepository alertRepository,
            BusinessServiceRepository serviceRepository) {
        this.groupRepository = groupRepository;
        this.alertRepository = alertRepository;
        this.serviceRepository = serviceRepository;
    }

    @Transactional(readOnly = true)
    public GetAlertGroupResult get(GetAlertGroupQuery query) {
        Objects.requireNonNull(query, "query must not be null");
        var group = groupRepository.findById(query.groupId())
                .orElseThrow(() -> new AlertGroupNotFoundException(query.groupId()));
        Map<Long, RootCandidateService> candidateServices = group.getRootAssessment() instanceof RootAssessment.Ranked ranked
                ? serviceRepository.findAllById(ranked.candidates().stream().map(candidate -> candidate.serviceId()).toList())
                    .stream().collect(Collectors.toMap(service -> service.getId(),
                            service -> new RootCandidateService(service.getId(), service.getCode(), service.getName())))
                : Map.of();
        return new GetAlertGroupResult(AlertGroupItem.from(group),
                alertRepository.findAllByGroup_IdOrderByOccurredAtAscIdAsc(query.groupId())
                        .stream().map(GroupAlertItem::from).toList(),
                group.getDecidedBy() == null ? null : group.getDecidedBy().getId(),
                group.getDecidedBy() == null ? null : group.getDecidedBy().getDisplayName(),
                group.getDecidedAt(), group.getDismissReason(), group.getVersion(), group.getRootAssessment(), candidateServices);
    }
}
