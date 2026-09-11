package ru.donskikh.incidenthub.catalog;

import ru.donskikh.incidenthub.common.DomainConflictException;

public class ServiceDependencyAlreadyExistsException extends DomainConflictException {

    public ServiceDependencyAlreadyExistsException(long dependentServiceId, long dependencyServiceId) {
        super("Service dependency already exists: "
                + dependentServiceId + " -> " + dependencyServiceId);
    }

    public ServiceDependencyAlreadyExistsException(
            long dependentServiceId,
            long dependencyServiceId,
            Throwable cause
    ) {
        super("Service dependency already exists: "
                + dependentServiceId + " -> " + dependencyServiceId, cause);
    }
}
