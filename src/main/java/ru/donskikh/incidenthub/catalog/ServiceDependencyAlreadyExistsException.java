package ru.donskikh.incidenthub.catalog;

public class ServiceDependencyAlreadyExistsException extends RuntimeException {

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
