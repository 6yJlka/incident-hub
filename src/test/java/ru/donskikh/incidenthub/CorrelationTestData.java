package ru.donskikh.incidenthub;

import org.springframework.test.util.ReflectionTestUtils;
import ru.donskikh.incidenthub.alert.correlation.Evidence;
import ru.donskikh.incidenthub.alert.correlation.RootAssessment;
import ru.donskikh.incidenthub.alert.correlation.RootCandidate;
import ru.donskikh.incidenthub.alertgroup.AlertGroup;
import ru.donskikh.incidenthub.catalog.BusinessService;

import java.time.Instant;
import java.util.List;

public final class CorrelationTestData {
    private CorrelationTestData() {}

    public static RootAssessment ranked(long serviceId) {
        return new RootAssessment.Ranked(List.of(new RootCandidate(serviceId, 1.0)), List.of(new Evidence.Migrated()));
    }

    public static RootAssessment rankedFor(BusinessService service) {
        if (service.getId() == null) {
            if (org.mockito.Mockito.mockingDetails(service).isMock()) {
                org.mockito.Mockito.when(service.getId()).thenReturn(1L);
            } else {
                ReflectionTestUtils.setField(service, "id", 1L);
            }
        }
        return ranked(service.getId());
    }

    public static AlertGroup group(BusinessService service, Instant time) {
        var assessment = rankedFor(service);
        return new AlertGroup(service, time, assessment);
    }
}
