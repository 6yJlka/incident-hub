package ru.donskikh.incidenthub.alert;

public interface AlertCorrelationRepository {

    boolean tryAcquireCorrelationLock();

    int attachToGroup(long alertId, long groupId);
}
