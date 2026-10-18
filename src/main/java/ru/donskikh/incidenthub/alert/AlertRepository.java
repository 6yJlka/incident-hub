package ru.donskikh.incidenthub.alert;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

public interface AlertRepository extends JpaRepository<Alert, Long>, AlertBatchRepository, AlertCorrelationRepository {

    long countByGroup_Id(long groupId);

    List<Alert> findAllByGroupIsNullOrderByOccurredAtAscIdAsc(Pageable pageable);

    @EntityGraph(attributePaths = "businessService")
    List<Alert> findAllByGroup_IdOrderByOccurredAtAscIdAsc(long groupId);

    @Query("""
            select alert.group.id as groupId, alert.businessService.id as businessServiceId,
                   alert.occurredAt as occurredAt, alert.signalType as signalType, alert.severity as severity
            from Alert alert
            where alert.group.id in (:groupIds)
            order by alert.occurredAt, alert.id
            """)
    List<CorrelationMemberProjection> findCorrelationMembers(@Param("groupIds") Collection<Long> groupIds);

    interface CorrelationMemberProjection {
        Long getGroupId();
        Long getBusinessServiceId();
        Instant getOccurredAt();
        AlertSignalType getSignalType();
        AlertSeverity getSeverity();
    }
}
