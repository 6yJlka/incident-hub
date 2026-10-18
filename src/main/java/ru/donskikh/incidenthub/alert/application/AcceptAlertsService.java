package ru.donskikh.incidenthub.alert.application;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import ru.donskikh.incidenthub.alert.AlertKey;
import ru.donskikh.incidenthub.alert.AlertProperties;
import ru.donskikh.incidenthub.alert.AlertRepository;
import ru.donskikh.incidenthub.alert.NewAlert;
import ru.donskikh.incidenthub.catalog.BusinessService;
import ru.donskikh.incidenthub.catalog.BusinessServiceRepository;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
@EnableConfigurationProperties(AlertProperties.class)
public class AcceptAlertsService {
    private final AlertRepository alertRepository;
    private final BusinessServiceRepository businessServiceRepository;
    private final AlertProperties properties;
    private final Clock clock;

    public AcceptAlertsService(AlertRepository alertRepository, BusinessServiceRepository businessServiceRepository,
                               AlertProperties properties, Clock clock) {
        this.alertRepository = alertRepository;
        this.businessServiceRepository = businessServiceRepository;
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public AcceptAlertsResult accept(AcceptAlertsCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        if (command.alerts().size() > properties.maxBatchSize()) {
            throw new IllegalArgumentException("Alert batch exceeds the configured maximum size");
        }
        Set<AlertKey> keys = new LinkedHashSet<>();
        command.alerts().forEach(alert -> keys.add(alert.key()));
        Map<AlertKey, Long> existing = alertRepository.findIdsByKeys(keys);
        Set<String> serviceCodes = new HashSet<>();
        command.alerts().stream().filter(alert -> !existing.containsKey(alert.key()))
                .forEach(alert -> serviceCodes.add(alert.serviceCode()));
        Map<String, BusinessService> services = new HashMap<>();
        if (!serviceCodes.isEmpty()) {
            businessServiceRepository.findAllByCodeIn(serviceCodes)
                    .forEach(service -> services.put(service.getCode(), service));
        }
        Map<AlertKey, NewAlert> candidates = new LinkedHashMap<>();
        Instant receivedAt = clock.instant();
        for (IncomingAlert alert : command.alerts()) {
            if (!existing.containsKey(alert.key()) && services.containsKey(alert.serviceCode())) {
                candidates.putIfAbsent(alert.key(), new NewAlert(
                        alert.key(), services.get(alert.serviceCode()).getId(), alert.signalType(),
                        alert.severity(), alert.occurredAt(), receivedAt, alert.description()
                ));
            }
        }
        Map<AlertKey, Long> inserted = candidates.isEmpty()
                ? Map.of() : alertRepository.insertIfAbsent(List.copyOf(candidates.values()));
        Map<AlertKey, Long> ids = new HashMap<>(existing);
        // A separate READ COMMITTED statement sees a concurrent winner after ON CONFLICT waits.
        if (!candidates.isEmpty()) {
            ids.putAll(alertRepository.findIdsByKeys(candidates.keySet()));
        }
        Set<AlertKey> reported = new HashSet<>(existing.keySet());
        List<AlertAcceptanceResult> results = new ArrayList<>();
        for (IncomingAlert alert : command.alerts()) {
            Long id = ids.get(alert.key());
            if (reported.contains(alert.key())) {
                results.add(new AlertAcceptanceResult(alert.externalId(), AlertAcceptanceStatus.DUPLICATE, id, null));
            } else if (!services.containsKey(alert.serviceCode())) {
                results.add(new AlertAcceptanceResult(alert.externalId(), AlertAcceptanceStatus.REJECTED,
                        null, AlertRejectionReason.UNKNOWN_SERVICE));
            } else {
                Objects.requireNonNull(id, "inserted or concurrent alert must exist");
                results.add(new AlertAcceptanceResult(alert.externalId(),
                        inserted.containsKey(alert.key()) ? AlertAcceptanceStatus.ACCEPTED : AlertAcceptanceStatus.DUPLICATE,
                        id, null));
                reported.add(alert.key());
            }
        }
        return new AcceptAlertsResult(results);
    }
}
