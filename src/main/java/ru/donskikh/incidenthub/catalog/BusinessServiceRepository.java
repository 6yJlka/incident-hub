package ru.donskikh.incidenthub.catalog;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface BusinessServiceRepository extends
        JpaRepository<BusinessService, Long>,
        JpaSpecificationExecutor<BusinessService> {

    @Query("""
            select count(service) > 0
            from BusinessService service
            where lower(trim(service.code)) = lower(trim(:code))
            """)
    boolean existsByNormalizedCode(@Param("code") String code);

    @Query(value = """
            select counts.service_id as "serviceId",
                   sum(counts.dependency_count) as "dependencyCount",
                   sum(counts.dependent_count) as "dependentCount",
                   sum(counts.active_incident_count) as "activeIncidentCount"
            from (
                select dependent_id as service_id, count(*) as dependency_count,
                       0 as dependent_count, 0 as active_incident_count
                from service_dependencies
                where dependent_id in (:serviceIds)
                group by dependent_id
                union all
                select dependency_id, 0, count(*), 0
                from service_dependencies
                where dependency_id in (:serviceIds)
                group by dependency_id
                union all
                select affected_service_id, 0, 0, count(*)
                from incidents
                where affected_service_id in (:serviceIds) and status in (:activeStatuses)
                group by affected_service_id
            ) counts
            group by counts.service_id
            """, nativeQuery = true)
    List<ServiceCountsProjection> findCounts(
            @Param("serviceIds") Collection<Long> serviceIds,
            @Param("activeStatuses") Collection<String> activeStatuses
    );

    interface ServiceCountsProjection {

        Long getServiceId();

        long getDependencyCount();

        long getDependentCount();

        long getActiveIncidentCount();
    }

    @Override
    @EntityGraph(attributePaths = "ownerTeam")
    Page<BusinessService> findAll(Specification<BusinessService> specification, Pageable pageable);
}
