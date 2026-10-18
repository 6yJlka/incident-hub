package ru.donskikh.incidenthub.alert;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface AlertBatchRepository {
    Map<AlertKey, Long> findIdsByKeys(Collection<AlertKey> keys);
    Map<AlertKey, Long> insertIfAbsent(List<NewAlert> alerts);
}
