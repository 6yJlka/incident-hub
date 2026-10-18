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
    @EntityGraph(attributePaths = "probableRootService")
    Optional<AlertGroup> findById(Long id);

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
