package ru.donskikh.incidenthub.alertgroup.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.donskikh.incidenthub.alert.AlertRepository;
import ru.donskikh.incidenthub.alertgroup.AlertGroupNotFoundException;
import ru.donskikh.incidenthub.alertgroup.AlertGroupRepository;

import java.util.Objects;

@Service
public class GetAlertGroupService {

    private final AlertGroupRepository groupRepository;
    private final AlertRepository alertRepository;

    public GetAlertGroupService(AlertGroupRepository groupRepository, AlertRepository alertRepository) {
        this.groupRepository = groupRepository;
        this.alertRepository = alertRepository;
    }

    @Transactional(readOnly = true)
    public GetAlertGroupResult get(GetAlertGroupQuery query) {
        Objects.requireNonNull(query, "query must not be null");
        var group = groupRepository.findById(query.groupId())
                .orElseThrow(() -> new AlertGroupNotFoundException(query.groupId()));
        return new GetAlertGroupResult(AlertGroupItem.from(group),
                alertRepository.findAllByGroup_IdOrderByOccurredAtAscIdAsc(query.groupId())
                        .stream().map(GroupAlertItem::from).toList(),
                group.getDecidedBy() == null ? null : group.getDecidedBy().getId(),
                group.getDecidedBy() == null ? null : group.getDecidedBy().getDisplayName(),
                group.getDecidedAt(), group.getDismissReason(), group.getVersion());
    }
}
