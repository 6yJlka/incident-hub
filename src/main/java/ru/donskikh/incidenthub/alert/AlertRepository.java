package ru.donskikh.incidenthub.alert;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AlertRepository extends JpaRepository<Alert, Long>, AlertBatchRepository {
}
