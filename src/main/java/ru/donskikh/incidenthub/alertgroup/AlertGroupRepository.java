package ru.donskikh.incidenthub.alertgroup;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface AlertGroupRepository extends JpaRepository<AlertGroup, Long>, JpaSpecificationExecutor<AlertGroup> {

    @Override
    @EntityGraph(attributePaths = "probableRootService")
    Page<AlertGroup> findAll(Specification<AlertGroup> specification, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"probableRootService", "decidedBy"})
    Optional<AlertGroup> findById(Long id);

    @Query("select g.id from AlertGroup g where g.incident.id = :incidentId")
    Optional<Long> findIdByIncidentId(@Param("incidentId") long incidentId);

    @Query(value = """
            with ordered_alerts as (
                select a.*, bs.code,
                       row_number() over (partition by a.group_id, a.business_service_id
                                          order by a.occurred_at, a.id) as service_occurrence
                from alerts a join business_services bs on bs.id = a.business_service_id
                where a.group_id in (:groupIds)
            ), service_previews as (
                select group_id, code,
                       row_number() over (partition by group_id order by occurred_at, id) as service_order
                from ordered_alerts where service_occurrence = 1
            ), previews as (
                select group_id, array_agg(code order by service_order) as codes
                from service_previews where service_order <= 3 group by group_id
            )
            select a.group_id as "groupId", count(*) as "alertCount",
                   count(distinct a.business_service_id) as "serviceCount", p.codes as "servicePreviewCodes",
                   case min(case a.severity when 'CRITICAL' then 0 when 'WARNING' then 1 else 2 end)
                       when 0 then 'CRITICAL' when 1 then 'WARNING' else 'INFO' end as "worstSeverity"
            from ordered_alerts a join previews p on p.group_id = a.group_id
            group by a.group_id, p.codes
            """, nativeQuery = true)
    List<SummaryProjection> findSummariesByGroupIds(@Param("groupIds") List<Long> groupIds);

    interface SummaryProjection {
        Long getGroupId();
        long getAlertCount();
        long getServiceCount();
        String[] getServicePreviewCodes();
        String getWorstSeverity();
    }

    @Query(value = """
            select * from alert_groups
            where status = 'OPEN'
              and last_alert_at >= :lowerBound
              and first_alert_at <= :upperBound
            order by id
            """, nativeQuery = true)
    List<AlertGroup> findCorrelationCandidates(
            @Param("lowerBound") Instant lowerBound,
            @Param("upperBound") Instant upperBound
    );
}
