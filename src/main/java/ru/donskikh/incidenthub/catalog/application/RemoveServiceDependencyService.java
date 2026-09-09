package ru.donskikh.incidenthub.catalog.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.donskikh.incidenthub.catalog.ServiceDependency;
import ru.donskikh.incidenthub.catalog.ServiceDependencyNotFoundException;
import ru.donskikh.incidenthub.catalog.ServiceDependencyRepository;

import java.util.Objects;

@Service
public class RemoveServiceDependencyService {

    private final ServiceDependencyRepository serviceDependencyRepository;

    public RemoveServiceDependencyService(ServiceDependencyRepository serviceDependencyRepository) {
        this.serviceDependencyRepository = serviceDependencyRepository;
    }

    @Transactional
    public RemoveServiceDependencyResult remove(RemoveServiceDependencyCommand command) {
        Objects.requireNonNull(command, "command must not be null");

        ServiceDependency relationship = serviceDependencyRepository
                .findByDependent_IdAndDependency_Id(
                        command.dependentServiceId(),
                        command.dependencyServiceId()
                )
                .orElseThrow(() -> new ServiceDependencyNotFoundException(
                        command.dependentServiceId(),
                        command.dependencyServiceId()
                ));

        serviceDependencyRepository.delete(relationship);

        return new RemoveServiceDependencyResult(
                command.dependentServiceId(),
                command.dependencyServiceId()
        );
    }
}
