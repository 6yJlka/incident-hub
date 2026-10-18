package ru.donskikh.incidenthub.alert.infrastructure;

import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import ru.donskikh.incidenthub.alert.AlertBatchRepository;
import ru.donskikh.incidenthub.alert.AlertKey;
import ru.donskikh.incidenthub.alert.NewAlert;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import java.util.stream.Collectors;

public class AlertBatchRepositoryImpl implements AlertBatchRepository {
    private final NamedParameterJdbcTemplate jdbcTemplate;

    public AlertBatchRepositoryImpl(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Map<AlertKey, Long> findIdsByKeys(Collection<AlertKey> keys) {
        if (keys.isEmpty()) {
            return Map.of();
        }
        List<AlertKey> orderedKeys = List.copyOf(keys);
        MapSqlParameterSource parameters = new MapSqlParameterSource();
        String values = IntStream.range(0, orderedKeys.size()).mapToObj(index -> {
            parameters.addValue("source" + index, orderedKeys.get(index).source());
            parameters.addValue("externalId" + index, orderedKeys.get(index).externalId());
            return "(:source" + index + ", :externalId" + index + ")";
        }).collect(Collectors.joining(", "));
        return queryIds("select source, external_id, id from alerts where (source, external_id) in (" + values + ")",
                parameters);
    }

    @Override
    public Map<AlertKey, Long> insertIfAbsent(List<NewAlert> alerts) {
        if (alerts.isEmpty()) {
            return Map.of();
        }
        // A consistent key order prevents opposing lock orders in overlapping batches.
        List<NewAlert> ordered = alerts.stream().sorted(Comparator
                .comparing((NewAlert alert) -> alert.key().source())
                .thenComparing(alert -> alert.key().externalId())).toList();
        MapSqlParameterSource parameters = new MapSqlParameterSource();
        String values = IntStream.range(0, ordered.size()).mapToObj(index -> {
            NewAlert alert = ordered.get(index);
            parameters.addValue("source" + index, alert.key().source());
            parameters.addValue("externalId" + index, alert.key().externalId());
            parameters.addValue("service" + index, alert.businessServiceId());
            parameters.addValue("signal" + index, alert.signalType().name());
            parameters.addValue("severity" + index, alert.severity().name());
            parameters.addValue("occurred" + index, Timestamp.from(alert.occurredAt()));
            parameters.addValue("received" + index, Timestamp.from(alert.receivedAt()));
            parameters.addValue("description" + index, alert.description());
            return "(:source%d, :externalId%d, :service%d, :signal%d, :severity%d, :occurred%d, :received%d, :description%d)"
                    .formatted(index, index, index, index, index, index, index, index);
        }).collect(Collectors.joining(", "));
        return queryIds("""
                insert into alerts (source, external_id, business_service_id, signal_type,
                                    severity, occurred_at, received_at, description)
                values %s
                on conflict (source, external_id) do nothing
                returning source, external_id, id
                """.formatted(values), parameters);
    }

    private Map<AlertKey, Long> queryIds(String sql, MapSqlParameterSource parameters) {
        Map<AlertKey, Long> result = new LinkedHashMap<>();
        jdbcTemplate.query(sql, parameters, (RowCallbackHandler) row ->
                result.put(new AlertKey(row.getString("source"), row.getString("external_id")), row.getLong("id")));
        return Map.copyOf(result);
    }
}
