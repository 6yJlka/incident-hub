package ru.donskikh.incidenthub.catalog;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BusinessServiceRepository extends
        JpaRepository<BusinessService, Long>,
        JpaSpecificationExecutor<BusinessService> {

    @Query("""
            select count(service) > 0
            from BusinessService service
            where lower(trim(service.code)) = lower(trim(:code))
            """)
    boolean existsByNormalizedCode(@Param("code") String code);

    @Override
    @EntityGraph(attributePaths = "ownerTeam")
    Page<BusinessService> findAll(Specification<BusinessService> specification, Pageable pageable);
}
