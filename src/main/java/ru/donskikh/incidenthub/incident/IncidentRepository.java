package ru.donskikh.incidenthub.incident;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface IncidentRepository extends JpaRepository<Incident, Long>, JpaSpecificationExecutor<Incident> {

    @Override
    @EntityGraph(attributePaths = {"reporter", "assignee", "responsibleTeam"})
    Page<Incident> findAll(Specification<Incident> specification, Pageable pageable);
}
