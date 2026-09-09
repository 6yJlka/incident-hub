package ru.donskikh.incidenthub.catalog;

public class ServiceSelfDependencyNotAllowedException extends RuntimeException {

    public ServiceSelfDependencyNotAllowedException(Long businessServiceId) {
        super(businessServiceId == null
                ? "A business service cannot depend on itself"
                : "Business service cannot depend on itself: " + businessServiceId);
    }
}
