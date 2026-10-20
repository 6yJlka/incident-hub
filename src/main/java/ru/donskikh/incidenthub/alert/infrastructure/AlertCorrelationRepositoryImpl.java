package ru.donskikh.incidenthub.alert.infrastructure;

import org.springframework.jdbc.core.JdbcTemplate;
import ru.donskikh.incidenthub.alert.AlertCorrelationRepository;
import ru.donskikh.incidenthub.alert.correlation.Evidence;
import ru.donskikh.incidenthub.alert.correlation.LinkStrength;

import java.util.List;

public class AlertCorrelationRepositoryImpl implements AlertCorrelationRepository {

    // Shared across all application instances in this database; reserved for alert correlation.
    private static final long CORRELATION_LOCK_KEY = 4_945_487_001L;

    private final JdbcTemplate jdbcTemplate;

    public AlertCorrelationRepositoryImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public boolean tryAcquireCorrelationLock() {
        return Boolean.TRUE.equals(jdbcTemplate.queryForObject(
                "select pg_try_advisory_xact_lock(?)", Boolean.class, CORRELATION_LOCK_KEY
        ));
    }

    @Override
    public int attachToGroup(long alertId, long groupId, LinkStrength linkStrength, List<Evidence> linkEvidence) {
        return jdbcTemplate.update(
                "update alerts set group_id = ?, link_strength = ?, link_evidence = ?::jsonb where id = ? and group_id is null",
                groupId, linkStrength == null ? null : linkStrength.name(), CorrelationJsonCodec.writeEvidence(linkEvidence), alertId
        );
    }
}
