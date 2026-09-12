package ru.donskikh.incidenthub.catalog;

import ru.donskikh.incidenthub.common.DomainNotFoundException;

public class ServiceDependencyNotFoundException extends DomainNotFoundException {

    public ServiceDependencyNotFoundException(long dependentServiceId, long dependencyServiceId) {
        super("Service dependency not found: "
                + dependentServiceId + " -> " + dependencyServiceId);
    }
}
