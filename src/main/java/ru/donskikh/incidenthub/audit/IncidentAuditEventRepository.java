package ru.donskikh.incidenthub.audit;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.List;

public interface IncidentAuditEventRepository extends JpaRepository<IncidentAuditEvent, Long> {

    @EntityGraph(attributePaths = "actor")
    List<IncidentAuditEvent> findByIncidentIdOrderByCreatedAtAscIdAsc(long incidentId);
}
