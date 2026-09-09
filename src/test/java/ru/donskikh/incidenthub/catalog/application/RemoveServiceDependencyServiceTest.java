package ru.donskikh.incidenthub.catalog.application;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.donskikh.incidenthub.catalog.ServiceDependency;
import ru.donskikh.incidenthub.catalog.ServiceDependencyNotFoundException;
import ru.donskikh.incidenthub.catalog.ServiceDependencyRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RemoveServiceDependencyServiceTest {

    @Mock
    private ServiceDependencyRepository serviceDependencyRepository;

    @InjectMocks
    private RemoveServiceDependencyService service;

    @Test
    void removesExistingServiceDependency() {
        ServiceDependency relationship = org.mockito.Mockito.mock(ServiceDependency.class);
        when(serviceDependencyRepository.findByDependent_IdAndDependency_Id(42L, 43L))
                .thenReturn(Optional.of(relationship));

        RemoveServiceDependencyResult result = service.remove(command());

        verify(serviceDependencyRepository).delete(relationship);
        assertThat(result.dependentServiceId()).isEqualTo(42L);
        assertThat(result.dependencyServiceId()).isEqualTo(43L);
    }

    @Test
    void throwsWhenServiceDependencyDoesNotExist() {
        when(serviceDependencyRepository.findByDependent_IdAndDependency_Id(42L, 43L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.remove(command()))
                .isInstanceOf(ServiceDependencyNotFoundException.class)
                .hasMessage("Service dependency not found: 42 -> 43");
    }

    @Test
    void rejectsNullCommand() {
        assertThatThrownBy(() -> service.remove(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("command must not be null");

        verifyNoInteractions(serviceDependencyRepository);
    }

    private static RemoveServiceDependencyCommand command() {
        return new RemoveServiceDependencyCommand(42L, 43L);
    }
}
