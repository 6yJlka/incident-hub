package ru.donskikh.incidenthub.catalog;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface BusinessServiceRepository extends
        JpaRepository<BusinessService, Long>,
        JpaSpecificationExecutor<BusinessService> {
}
