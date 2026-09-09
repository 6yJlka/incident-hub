package ru.donskikh.incidenthub.catalog;

public class ServiceDependencyNotFoundException extends RuntimeException {

    public ServiceDependencyNotFoundException(long dependentServiceId, long dependencyServiceId) {
        super("Service dependency not found: "
                + dependentServiceId + " -> " + dependencyServiceId);
    }
}
