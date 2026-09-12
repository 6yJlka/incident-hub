package ru.donskikh.incidenthub.catalog;

import ru.donskikh.incidenthub.common.DomainConflictException;

public class ServiceSelfDependencyNotAllowedException extends DomainConflictException {

    public ServiceSelfDependencyNotAllowedException(Long businessServiceId) {
        super(businessServiceId == null
                ? "A business service cannot depend on itself"
                : "Business service cannot depend on itself: " + businessServiceId);
    }
}
