package ru.donskikh.incidenthub.catalog;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ServiceDependencyRepository extends JpaRepository<ServiceDependency, Long> {

    @EntityGraph(attributePaths = "dependency")
    List<ServiceDependency> findAllByDependent_IdOrderByIdAsc(long dependentServiceId);

    Optional<ServiceDependency> findByDependent_IdAndDependency_Id(
            long dependentServiceId,
            long dependencyServiceId
    );
}
