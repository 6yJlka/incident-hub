package ru.donskikh.incidenthub.catalog.application;

import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import ru.donskikh.incidenthub.catalog.BusinessService;
import ru.donskikh.incidenthub.catalog.BusinessServiceNotFoundException;
import ru.donskikh.incidenthub.catalog.BusinessServiceRepository;
import ru.donskikh.incidenthub.catalog.DependencyType;
import ru.donskikh.incidenthub.catalog.ServiceDependency;
import ru.donskikh.incidenthub.catalog.ServiceDependencyAlreadyExistsException;
import ru.donskikh.incidenthub.catalog.ServiceDependencyRepository;
import ru.donskikh.incidenthub.catalog.ServiceSelfDependencyNotAllowedException;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AddServiceDependencyServiceTest {

    @Mock
    private BusinessServiceRepository businessServiceRepository;

    @Mock
    private ServiceDependencyRepository serviceDependencyRepository;

    @InjectMocks
    private AddServiceDependencyService service;

    @Test
    void addsServiceDependency() {
        BusinessService dependent = businessService(42L);
        BusinessService dependency = businessService(43L);
        ServiceDependency savedRelationship = mock(ServiceDependency.class);
        Instant createdAt = Instant.parse("2026-09-01T10:00:00Z");
        when(businessServiceRepository.findById(42L)).thenReturn(Optional.of(dependent));
        when(businessServiceRepository.findById(43L)).thenReturn(Optional.of(dependency));
        when(serviceDependencyRepository.findByDependent_IdAndDependency_Id(42L, 43L))
                .thenReturn(Optional.empty());
        when(serviceDependencyRepository.saveAndFlush(any(ServiceDependency.class)))
                .thenReturn(savedRelationship);
        when(savedRelationship.getId()).thenReturn(100L);
        when(savedRelationship.getDependent()).thenReturn(dependent);
        when(savedRelationship.getDependency()).thenReturn(dependency);
        when(savedRelationship.getType()).thenReturn(DependencyType.SYNC);
        when(savedRelationship.getCreatedAt()).thenReturn(createdAt);

        AddServiceDependencyResult result = service.add(command());

        ArgumentCaptor<ServiceDependency> relationshipCaptor = ArgumentCaptor.forClass(ServiceDependency.class);
        verify(serviceDependencyRepository).saveAndFlush(relationshipCaptor.capture());
        ServiceDependency created = relationshipCaptor.getValue();
        assertThat(created.getDependent()).isSameAs(dependent);
        assertThat(created.getDependency()).isSameAs(dependency);
        assertThat(created.getType()).isEqualTo(DependencyType.SYNC);
        assertThat(result.relationshipId()).isEqualTo(100L);
        assertThat(result.dependentServiceId()).isEqualTo(42L);
        assertThat(result.dependencyServiceId()).isEqualTo(43L);
        assertThat(result.type()).isEqualTo(DependencyType.SYNC);
        assertThat(result.createdAt()).isEqualTo(createdAt);
    }

    @Test
    void throwsWhenDependentBusinessServiceDoesNotExist() {
        when(businessServiceRepository.findById(42L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.add(command()))
                .isInstanceOf(BusinessServiceNotFoundException.class)
                .hasMessage("Business service not found: 42");

        verify(businessServiceRepository, never()).findById(43L);
        verifyNoInteractions(serviceDependencyRepository);
    }

    @Test
    void throwsWhenDependencyBusinessServiceDoesNotExist() {
        BusinessService dependent = mock(BusinessService.class);
        when(businessServiceRepository.findById(42L)).thenReturn(Optional.of(dependent));
        when(businessServiceRepository.findById(43L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.add(command()))
                .isInstanceOf(BusinessServiceNotFoundException.class)
                .hasMessage("Business service not found: 43");

        verifyNoInteractions(serviceDependencyRepository);
    }

    @Test
    void propagatesDomainExceptionForSelfReference() {
        BusinessService businessService = businessService(42L);
        when(businessServiceRepository.findById(42L)).thenReturn(Optional.of(businessService));

        assertThatThrownBy(() -> service.add(new AddServiceDependencyCommand(
                42L, 42L, DependencyType.SYNC
        )))
                .isInstanceOf(ServiceSelfDependencyNotAllowedException.class)
                .hasMessage("Business service cannot depend on itself: 42");

        verifyNoInteractions(serviceDependencyRepository);
    }

    @Test
    void throwsDomainExceptionWhenDependencyAlreadyExists() {
        BusinessService dependent = businessService(42L);
        BusinessService dependency = businessService(43L);
        when(businessServiceRepository.findById(42L)).thenReturn(Optional.of(dependent));
        when(businessServiceRepository.findById(43L)).thenReturn(Optional.of(dependency));
        when(serviceDependencyRepository.findByDependent_IdAndDependency_Id(42L, 43L))
                .thenReturn(Optional.of(mock(ServiceDependency.class)));

        assertThatThrownBy(() -> service.add(command()))
                .isInstanceOf(ServiceDependencyAlreadyExistsException.class)
                .hasMessage("Service dependency already exists: 42 -> 43");

        verify(serviceDependencyRepository, never()).saveAndFlush(any(ServiceDependency.class));
    }

    @Test
    void translatesConcurrentPairConflictToDomainException() {
        BusinessService dependent = businessService(42L);
        BusinessService dependency = businessService(43L);
        DataIntegrityViolationException databaseException = databaseException(
                "uk_service_dependencies_pair"
        );
        when(businessServiceRepository.findById(42L)).thenReturn(Optional.of(dependent));
        when(businessServiceRepository.findById(43L)).thenReturn(Optional.of(dependency));
        when(serviceDependencyRepository.findByDependent_IdAndDependency_Id(42L, 43L))
                .thenReturn(Optional.empty());
        when(serviceDependencyRepository.saveAndFlush(any(ServiceDependency.class))).thenThrow(databaseException);

        assertThatThrownBy(() -> service.add(command()))
                .isInstanceOf(ServiceDependencyAlreadyExistsException.class)
                .hasMessage("Service dependency already exists: 42 -> 43")
                .hasCause(databaseException);
    }

    @Test
    void doesNotTranslateDifferentIntegrityConstraint() {
        BusinessService dependent = businessService(42L);
        BusinessService dependency = businessService(43L);
        DataIntegrityViolationException databaseException = databaseException(
                "fk_service_dependencies_dependency"
        );
        when(businessServiceRepository.findById(42L)).thenReturn(Optional.of(dependent));
        when(businessServiceRepository.findById(43L)).thenReturn(Optional.of(dependency));
        when(serviceDependencyRepository.findByDependent_IdAndDependency_Id(42L, 43L))
                .thenReturn(Optional.empty());
        when(serviceDependencyRepository.saveAndFlush(any(ServiceDependency.class))).thenThrow(databaseException);

        assertThatThrownBy(() -> service.add(command())).isSameAs(databaseException);
    }

    @Test
    void doesNotTranslateIntegrityViolationWithoutConstraintName() {
        BusinessService dependent = businessService(42L);
        BusinessService dependency = businessService(43L);
        DataIntegrityViolationException databaseException = databaseException(null);
        when(businessServiceRepository.findById(42L)).thenReturn(Optional.of(dependent));
        when(businessServiceRepository.findById(43L)).thenReturn(Optional.of(dependency));
        when(serviceDependencyRepository.findByDependent_IdAndDependency_Id(42L, 43L))
                .thenReturn(Optional.empty());
        when(serviceDependencyRepository.saveAndFlush(any(ServiceDependency.class))).thenThrow(databaseException);

        assertThatThrownBy(() -> service.add(command())).isSameAs(databaseException);
    }

    @Test
    void rejectsNullCommand() {
        assertThatThrownBy(() -> service.add(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("command must not be null");

        verifyNoInteractions(businessServiceRepository, serviceDependencyRepository);
    }

    private static AddServiceDependencyCommand command() {
        return new AddServiceDependencyCommand(42L, 43L, DependencyType.SYNC);
    }

    private static BusinessService businessService(long id) {
        BusinessService businessService = mock(BusinessService.class);
        when(businessService.getId()).thenReturn(id);
        return businessService;
    }

    private static DataIntegrityViolationException databaseException(String constraintName) {
        ConstraintViolationException constraintViolation = mock(ConstraintViolationException.class);
        when(constraintViolation.getConstraintName()).thenReturn(constraintName);
        return new DataIntegrityViolationException("integrity violation", constraintViolation);
    }
}
