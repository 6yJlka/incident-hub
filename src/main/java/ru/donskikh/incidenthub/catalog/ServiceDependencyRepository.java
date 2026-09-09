package ru.donskikh.incidenthub.catalog;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ServiceDependencyRepository extends JpaRepository<ServiceDependency, Long> {

    @EntityGraph(attributePaths = "dependency")
    List<ServiceDependency> findAllByDependent_IdOrderByIdAsc(long dependentServiceId);

    Optional<ServiceDependency> findByDependent_IdAndDependency_Id(
            long dependentServiceId,
            long dependencyServiceId
    );

    @Query(value = """
            with recursive affected_paths (service_id, depth, edge_type, path) as (
                select
                    sd.dependent_id,
                    1,
                    sd.type,
                    array[:businessServiceId, sd.dependent_id]::bigint[]
                from service_dependencies sd
                where sd.dependency_id = :businessServiceId

                union all

                select
                    sd.dependent_id,
                    affected_paths.depth + 1,
                    sd.type,
                    affected_paths.path || sd.dependent_id
                from affected_paths
                join service_dependencies sd
                    on sd.dependency_id = affected_paths.service_id
                where affected_paths.depth < :maxDepth
                  and not (sd.dependent_id = any(affected_paths.path))
            ),
            first_reaches as (
                select
                    service_id,
                    depth,
                    edge_type,
                    row_number() over (
                        partition by service_id
                        order by depth, path, edge_type
                    ) as reach_order
                from affected_paths
            )
            select
                bs.id as id,
                bs.code as code,
                bs.name as name,
                bs.tier as tier,
                bs.active as active,
                team.id as "ownerTeamId",
                team.name as "ownerTeamName",
                first_reaches.depth as depth,
                first_reaches.edge_type as "dependencyType"
            from first_reaches
            join business_services bs on bs.id = first_reaches.service_id
            join teams team on team.id = bs.owner_team_id
            where first_reaches.reach_order = 1
              and bs.id <> :businessServiceId
            order by first_reaches.depth, bs.code
            """, nativeQuery = true)
    List<AffectedServiceProjection> findAffectedServices(
            @Param("businessServiceId") long businessServiceId,
            @Param("maxDepth") int maxDepth
    );

    interface AffectedServiceProjection {

        Long getId();

        String getCode();

        String getName();

        String getTier();

        boolean getActive();

        Long getOwnerTeamId();

        String getOwnerTeamName();

        int getDepth();

        String getDependencyType();
    }
}
