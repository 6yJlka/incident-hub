package ru.donskikh.incidenthub.alertgroup.application;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.donskikh.incidenthub.alertgroup.AlertGroupRepository;
import ru.donskikh.incidenthub.alertgroup.infrastructure.AlertGroupSpecifications;

import java.util.Objects;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ListAlertGroupsService {

    private final AlertGroupRepository repository;

    public ListAlertGroupsService(AlertGroupRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public ListAlertGroupsResult execute(ListAlertGroupsQuery query) {
        Objects.requireNonNull(query, "query must not be null");
        var page = repository.findAll(AlertGroupSpecifications.withFilters(query.status(), query.rootServiceId()),
                PageRequest.of(query.page(), query.size(),
                        Sort.by(Sort.Order.desc("lastAlertAt"), Sort.Order.desc("id"))));
        Map<Long, AlertGroupRepository.SummaryProjection> summaries = page.isEmpty() ? Map.of()
                : repository.findSummariesByGroupIds(page.getContent().stream().map(group -> group.getId()).toList())
                    .stream().collect(Collectors.toMap(AlertGroupRepository.SummaryProjection::getGroupId, Function.identity()));
        return new ListAlertGroupsResult(page.getContent().stream()
                .map(group -> AlertGroupItem.from(group).withSummary(summaries.get(group.getId()))).toList(),
                page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages(),
                page.hasNext(), page.hasPrevious());
    }
}
