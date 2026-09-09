package ru.donskikh.incidenthub.catalog.application;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.donskikh.incidenthub.catalog.BusinessService;
import ru.donskikh.incidenthub.catalog.BusinessServiceNotFoundException;
import ru.donskikh.incidenthub.catalog.BusinessServiceRepository;
import ru.donskikh.incidenthub.catalog.DependencyType;
import ru.donskikh.incidenthub.catalog.ServiceDependency;
import ru.donskikh.incidenthub.catalog.ServiceDependencyRepository;
import ru.donskikh.incidenthub.catalog.ServiceTier;
import ru.donskikh.incidenthub.team.Team;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetBusinessServiceServiceTest {

    @Mock
    private BusinessServiceRepository businessServiceRepository;

    @Mock
    private ServiceDependencyRepository serviceDependencyRepository;

    @InjectMocks
    private GetBusinessServiceService service;

    @Test
    void returnsBusinessServiceWithOwnerAndDirectDependencies() {
        BusinessService businessService = mock(BusinessService.class);
        BusinessService dependency = mock(BusinessService.class);
        ServiceDependency relationship = mock(ServiceDependency.class);
        Team ownerTeam = mock(Team.class);
        Instant createdAt = Instant.parse("2026-09-01T10:00:00Z");
        Instant updatedAt = Instant.parse("2026-09-02T11:00:00Z");
        Instant dependencyCreatedAt = Instant.parse("2026-09-03T12:00:00Z");
        when(businessServiceRepository.findById(42L)).thenReturn(Optional.of(businessService));
        when(businessService.getId()).thenReturn(42L);
        when(businessService.getCode()).thenReturn("CHECKOUT");
        when(businessService.getName()).thenReturn("Checkout");
        when(businessService.getDescription()).thenReturn("Checkout service");
        when(businessService.getOwnerTeam()).thenReturn(ownerTeam);
        when(businessService.getTier()).thenReturn(ServiceTier.TIER_1);
        when(businessService.isActive()).thenReturn(true);
        when(businessService.getCreatedAt()).thenReturn(createdAt);
        when(businessService.getUpdatedAt()).thenReturn(updatedAt);
        when(ownerTeam.getId()).thenReturn(7L);
        when(ownerTeam.getName()).thenReturn("Platform");
        when(ownerTeam.getCode()).thenReturn("PLATFORM");
        when(serviceDependencyRepository.findAllByDependent_IdOrderByIdAsc(42L))
                .thenReturn(List.of(relationship));
        when(relationship.getId()).thenReturn(100L);
        when(relationship.getDependency()).thenReturn(dependency);
        when(relationship.getType()).thenReturn(DependencyType.SYNC);
        when(relationship.getCreatedAt()).thenReturn(dependencyCreatedAt);
        when(dependency.getId()).thenReturn(43L);
        when(dependency.getCode()).thenReturn("BILLING");
        when(dependency.getName()).thenReturn("Billing");
        when(dependency.getTier()).thenReturn(ServiceTier.TIER_2);
        when(dependency.isActive()).thenReturn(true);

        GetBusinessServiceResult result = service.get(new GetBusinessServiceQuery(42L));

        assertThat(result.id()).isEqualTo(42L);
        assertThat(result.code()).isEqualTo("CHECKOUT");
        assertThat(result.ownerTeamId()).isEqualTo(7L);
        assertThat(result.ownerTeamName()).isEqualTo("Platform");
        assertThat(result.ownerTeamCode()).isEqualTo("PLATFORM");
        assertThat(result.createdAt()).isEqualTo(createdAt);
        assertThat(result.updatedAt()).isEqualTo(updatedAt);
        assertThat(result.dependencies()).singleElement().satisfies(item -> {
            assertThat(item.relationshipId()).isEqualTo(100L);
            assertThat(item.serviceId()).isEqualTo(43L);
            assertThat(item.code()).isEqualTo("BILLING");
            assertThat(item.name()).isEqualTo("Billing");
            assertThat(item.tier()).isEqualTo(ServiceTier.TIER_2);
            assertThat(item.active()).isTrue();
            assertThat(item.type()).isEqualTo(DependencyType.SYNC);
            assertThat(item.createdAt()).isEqualTo(dependencyCreatedAt);
        });
    }

    @Test
    void returnsEmptyDependencyList() {
        BusinessService businessService = mock(BusinessService.class);
        Team ownerTeam = mock(Team.class);
        when(businessServiceRepository.findById(42L)).thenReturn(Optional.of(businessService));
        when(businessService.getId()).thenReturn(42L);
        when(businessService.getOwnerTeam()).thenReturn(ownerTeam);
        when(serviceDependencyRepository.findAllByDependent_IdOrderByIdAsc(42L)).thenReturn(List.of());

        GetBusinessServiceResult result = service.get(new GetBusinessServiceQuery(42L));

        assertThat(result.dependencies()).isEmpty();
    }

    @Test
    void throwsWhenBusinessServiceDoesNotExist() {
        when(businessServiceRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(new GetBusinessServiceQuery(99L)))
                .isInstanceOf(BusinessServiceNotFoundException.class)
                .hasMessage("Business service not found: 99");

        verifyNoInteractions(serviceDependencyRepository);
    }

    @Test
    void rejectsNullQuery() {
        assertThatThrownBy(() -> service.get(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("query must not be null");

        verifyNoInteractions(businessServiceRepository, serviceDependencyRepository);
    }
}
