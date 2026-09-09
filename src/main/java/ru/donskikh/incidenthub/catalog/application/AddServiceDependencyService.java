package ru.donskikh.incidenthub.catalog.application;

import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.donskikh.incidenthub.catalog.BusinessService;
import ru.donskikh.incidenthub.catalog.BusinessServiceNotFoundException;
import ru.donskikh.incidenthub.catalog.BusinessServiceRepository;
import ru.donskikh.incidenthub.catalog.ServiceDependency;
import ru.donskikh.incidenthub.catalog.ServiceDependencyAlreadyExistsException;
import ru.donskikh.incidenthub.catalog.ServiceDependencyRepository;

import java.util.Objects;

@Service
public class AddServiceDependencyService {

    private static final String PAIR_UNIQUE_CONSTRAINT = "uk_service_dependencies_pair";

    private final BusinessServiceRepository businessServiceRepository;
    private final ServiceDependencyRepository serviceDependencyRepository;

    public AddServiceDependencyService(
            BusinessServiceRepository businessServiceRepository,
            ServiceDependencyRepository serviceDependencyRepository
    ) {
        this.businessServiceRepository = businessServiceRepository;
        this.serviceDependencyRepository = serviceDependencyRepository;
    }

    @Transactional
    public AddServiceDependencyResult add(AddServiceDependencyCommand command) {
        Objects.requireNonNull(command, "command must not be null");

        BusinessService dependent = businessServiceRepository.findById(command.dependentServiceId())
                .orElseThrow(() -> new BusinessServiceNotFoundException(command.dependentServiceId()));
        BusinessService dependency = businessServiceRepository.findById(command.dependencyServiceId())
                .orElseThrow(() -> new BusinessServiceNotFoundException(command.dependencyServiceId()));
        ServiceDependency relationship = new ServiceDependency(dependent, dependency, command.type());

        if (serviceDependencyRepository.findByDependent_IdAndDependency_Id(
                command.dependentServiceId(),
                command.dependencyServiceId()
        ).isPresent()) {
            throw new ServiceDependencyAlreadyExistsException(
                    command.dependentServiceId(),
                    command.dependencyServiceId()
            );
        }

        ServiceDependency savedRelationship;
        try {
            savedRelationship = serviceDependencyRepository.saveAndFlush(relationship);
        } catch (DataIntegrityViolationException exception) {
            if (exception.getCause() instanceof ConstraintViolationException constraintViolation
                    && PAIR_UNIQUE_CONSTRAINT.equals(constraintViolation.getConstraintName())) {
                throw new ServiceDependencyAlreadyExistsException(
                        command.dependentServiceId(),
                        command.dependencyServiceId(),
                        exception
                );
            }
            throw exception;
        }

        return new AddServiceDependencyResult(
                savedRelationship.getId(),
                savedRelationship.getDependent().getId(),
                savedRelationship.getDependency().getId(),
                savedRelationship.getType(),
                savedRelationship.getCreatedAt()
        );
    }
}
