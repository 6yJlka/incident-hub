package ru.donskikh.incidenthub.team;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TeamRepository extends JpaRepository<Team, Long> {

    @Query("""
            select count(team) > 0
            from Team team
            where lower(trim(team.code)) = lower(trim(:code))
            """)
    boolean existsByNormalizedCode(@Param("code") String code);

    Page<Team> findAllByActive(boolean active, Pageable pageable);
}
