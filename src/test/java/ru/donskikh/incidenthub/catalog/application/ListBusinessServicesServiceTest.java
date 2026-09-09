package ru.donskikh.incidenthub.catalog.application;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import ru.donskikh.incidenthub.catalog.BusinessService;
import ru.donskikh.incidenthub.catalog.BusinessServiceRepository;
import ru.donskikh.incidenthub.catalog.ServiceTier;
import ru.donskikh.incidenthub.team.Team;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListBusinessServicesServiceTest {

    @Mock
    private BusinessServiceRepository businessServiceRepository;

    @InjectMocks
    private ListBusinessServicesService service;

    @Test
    void passesSpecificationAndStablePageRequestAndReturnsMetadata() {
        ListBusinessServicesQuery query = new ListBusinessServicesQuery(
                1, 10, 7L, ServiceTier.TIER_1, true
        );
        when(businessServiceRepository.findAll(
                ArgumentMatchers.<Specification<BusinessService>>any(), any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of(), PageRequest.of(1, 10), 25));

        ListBusinessServicesResult result = service.execute(query);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Specification<BusinessService>> specificationCaptor =
                ArgumentCaptor.forClass(Specification.class);
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(businessServiceRepository).findAll(specificationCaptor.capture(), pageableCaptor.capture());
        Pageable pageable = pageableCaptor.getValue();
        assertThat(specificationCaptor.getValue()).isNotNull();
        assertThat(pageable.getPageNumber()).isEqualTo(1);
        assertThat(pageable.getPageSize()).isEqualTo(10);
        assertThat(pageable.getSort().toList()).extracting(Sort.Order::getProperty)
                .containsExactly("name", "id");
        assertThat(pageable.getSort().toList()).extracting(Sort.Order::getDirection)
                .containsExactly(Sort.Direction.ASC, Sort.Direction.ASC);
        assertThat(result.totalElements()).isEqualTo(25);
        assertThat(result.totalPages()).isEqualTo(3);
        assertThat(result.hasNext()).isTrue();
        assertThat(result.hasPrevious()).isTrue();
    }

    @Test
    void mapsBusinessServiceAndOwnerTeam() {
        BusinessService businessService = mock(BusinessService.class);
        Team ownerTeam = mock(Team.class);
        Instant createdAt = Instant.parse("2026-09-01T10:00:00Z");
        Instant updatedAt = Instant.parse("2026-09-02T11:00:00Z");
        when(businessServiceRepository.findAll(
                ArgumentMatchers.<Specification<BusinessService>>any(), any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of(businessService), PageRequest.of(0, 20), 1));
        when(businessService.getId()).thenReturn(42L);
        when(businessService.getCode()).thenReturn("BILLING");
        when(businessService.getName()).thenReturn("Billing");
        when(businessService.getOwnerTeam()).thenReturn(ownerTeam);
        when(businessService.getTier()).thenReturn(ServiceTier.TIER_1);
        when(businessService.isActive()).thenReturn(true);
        when(businessService.getCreatedAt()).thenReturn(createdAt);
        when(businessService.getUpdatedAt()).thenReturn(updatedAt);
        when(ownerTeam.getId()).thenReturn(7L);
        when(ownerTeam.getName()).thenReturn("Platform");
        when(ownerTeam.getCode()).thenReturn("PLATFORM");

        ListBusinessServiceItem item = service.execute(emptyQuery()).items().getFirst();

        assertThat(item.id()).isEqualTo(42L);
        assertThat(item.code()).isEqualTo("BILLING");
        assertThat(item.name()).isEqualTo("Billing");
        assertThat(item.ownerTeamId()).isEqualTo(7L);
        assertThat(item.ownerTeamName()).isEqualTo("Platform");
        assertThat(item.ownerTeamCode()).isEqualTo("PLATFORM");
        assertThat(item.tier()).isEqualTo(ServiceTier.TIER_1);
        assertThat(item.active()).isTrue();
        assertThat(item.createdAt()).isEqualTo(createdAt);
        assertThat(item.updatedAt()).isEqualTo(updatedAt);
    }

    @Test
    void rejectsNullQuery() {
        assertThatThrownBy(() -> service.execute(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("query must not be null");

        verifyNoInteractions(businessServiceRepository);
    }

    private static ListBusinessServicesQuery emptyQuery() {
        return new ListBusinessServicesQuery(0, 20, null, null, null);
    }
}
