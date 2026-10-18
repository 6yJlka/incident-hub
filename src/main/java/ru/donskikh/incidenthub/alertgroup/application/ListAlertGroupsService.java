package ru.donskikh.incidenthub.alertgroup.application;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.donskikh.incidenthub.alertgroup.AlertGroupRepository;
import ru.donskikh.incidenthub.alertgroup.infrastructure.AlertGroupSpecifications;

import java.util.Objects;

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
        return new ListAlertGroupsResult(page.getContent().stream().map(AlertGroupItem::from).toList(),
                page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages(),
                page.hasNext(), page.hasPrevious());
    }
}
