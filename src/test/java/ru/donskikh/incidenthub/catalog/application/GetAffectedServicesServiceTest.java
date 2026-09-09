package ru.donskikh.incidenthub.catalog.application;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.donskikh.incidenthub.catalog.BusinessServiceNotFoundException;
import ru.donskikh.incidenthub.catalog.BusinessServiceRepository;
import ru.donskikh.incidenthub.catalog.DependencyType;
import ru.donskikh.incidenthub.catalog.ServiceDependencyRepository;
import ru.donskikh.incidenthub.catalog.ServiceTier;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetAffectedServicesServiceTest {

    @Mock
    private BusinessServiceRepository businessServiceRepository;

    @Mock
    private ServiceDependencyRepository serviceDependencyRepository;

    @InjectMocks
    private GetAffectedServicesService service;

    @Test
    void returnsMappedAffectedServicesSortedByDepthAndCode() {
        ServiceDependencyRepository.AffectedServiceProjection depthTwo = projection(
                44L, "ORDERS", 2, "ASYNC"
        );
        ServiceDependencyRepository.AffectedServiceProjection depthOneSecond = projection(
                43L, "CHECKOUT", 1, "SYNC"
        );
        ServiceDependencyRepository.AffectedServiceProjection depthOneFirst = projection(
                45L, "BILLING", 1, "DATA"
        );
        when(businessServiceRepository.existsById(42L)).thenReturn(true);
        when(serviceDependencyRepository.findAffectedServices(42L, 3))
                .thenReturn(List.of(depthTwo, depthOneSecond, depthOneFirst));

        GetAffectedServicesResult result = service.get(new GetAffectedServicesQuery(42L, 3));

        assertThat(result.businessServiceId()).isEqualTo(42L);
        assertThat(result.maxDepth()).isEqualTo(3);
        assertThat(result.items()).extracting(AffectedServiceItem::code)
                .containsExactly("BILLING", "CHECKOUT", "ORDERS");
        assertThat(result.items()).extracting(AffectedServiceItem::depth)
                .containsExactly(1, 1, 2);
        assertThat(result.items()).extracting(AffectedServiceItem::dependencyType)
                .containsExactly(DependencyType.DATA, DependencyType.SYNC, DependencyType.ASYNC);
        assertThat(result.items()).allSatisfy(item -> {
            assertThat(item.tier()).isEqualTo(ServiceTier.TIER_2);
            assertThat(item.active()).isTrue();
            assertThat(item.ownerTeamId()).isEqualTo(7L);
            assertThat(item.ownerTeamName()).isEqualTo("Platform");
        });
    }

    @Test
    void throwsWhenBusinessServiceDoesNotExist() {
        when(businessServiceRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> service.get(new GetAffectedServicesQuery(99L, 3)))
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

    private static ServiceDependencyRepository.AffectedServiceProjection projection(
            long id,
            String code,
            int depth,
            String dependencyType
    ) {
        ServiceDependencyRepository.AffectedServiceProjection projection =
                mock(ServiceDependencyRepository.AffectedServiceProjection.class);
        when(projection.getId()).thenReturn(id);
        when(projection.getCode()).thenReturn(code);
        when(projection.getName()).thenReturn(code + " service");
        when(projection.getTier()).thenReturn("TIER_2");
        when(projection.getActive()).thenReturn(true);
        when(projection.getOwnerTeamId()).thenReturn(7L);
        when(projection.getOwnerTeamName()).thenReturn("Platform");
        when(projection.getDepth()).thenReturn(depth);
        when(projection.getDependencyType()).thenReturn(dependencyType);
        return projection;
    }
}
