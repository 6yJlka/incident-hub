package ru.donskikh.incidenthub.alert;

import ru.donskikh.incidenthub.alert.correlation.Evidence;
import ru.donskikh.incidenthub.alert.correlation.LinkStrength;

import java.util.List;

public interface AlertCorrelationRepository {

    boolean tryAcquireCorrelationLock();

    int attachToGroup(long alertId, long groupId, LinkStrength linkStrength, List<Evidence> linkEvidence);
}
