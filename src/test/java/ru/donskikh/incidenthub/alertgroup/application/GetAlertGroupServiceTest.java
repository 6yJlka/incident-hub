package ru.donskikh.incidenthub.alertgroup.application;

import org.junit.jupiter.api.Test;
import ru.donskikh.incidenthub.alert.AlertRepository;
import ru.donskikh.incidenthub.alertgroup.AlertGroup;
import ru.donskikh.incidenthub.alertgroup.AlertGroupNotFoundException;
import ru.donskikh.incidenthub.alertgroup.AlertGroupRepository;
import ru.donskikh.incidenthub.catalog.BusinessService;
import ru.donskikh.incidenthub.catalog.BusinessServiceRepository;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class GetAlertGroupServiceTest {

    private final AlertGroupRepository groups = mock(AlertGroupRepository.class);
    private final AlertRepository alerts = mock(AlertRepository.class);
    private final BusinessServiceRepository services = mock(BusinessServiceRepository.class);
    private final GetAlertGroupService service = new GetAlertGroupService(groups, alerts, services);

    @Test
    void returnsGroupWithOrderedAlertQuery() {
        var root = mock(BusinessService.class);
        when(root.getId()).thenReturn(42L);
        when(root.getCode()).thenReturn("ROOT");
        when(root.getName()).thenReturn("Root");
        var group = ru.donskikh.incidenthub.CorrelationTestData.group(root, Instant.parse("2026-10-18T12:00:00Z"));
        org.springframework.test.util.ReflectionTestUtils.setField(group, "id", 7L);
        org.springframework.test.util.ReflectionTestUtils.setField(group, "version", 12L);
        when(groups.findById(7L)).thenReturn(Optional.of(group));

        when(services.findAllById(java.util.List.of(42L))).thenReturn(java.util.List.of(root));
        var result = service.get(new GetAlertGroupQuery(7));

        assertThat(result.group().probableRootServiceId()).isEqualTo(42);
        assertThat(result.version()).isEqualTo(12);
        assertThat(result.rootCandidateServices().get(42L).code()).isEqualTo("ROOT");
        verify(services).findAllById(java.util.List.of(42L));
        assertThat(result.alerts()).isEmpty();
        verify(alerts).findAllByGroup_IdOrderByOccurredAtAscIdAsc(7);
    }

    @Test
    void rejectsMissingGroupWithoutLoadingAlerts() {
        assertThatThrownBy(() -> service.get(new GetAlertGroupQuery(7)))
                .isInstanceOf(AlertGroupNotFoundException.class);
        verifyNoInteractions(alerts);
    }

    @Test
    void rejectsNullQueryWithoutDatabaseAccess() {
        assertThatThrownBy(() -> service.get(null)).isInstanceOf(NullPointerException.class);
        verifyNoInteractions(groups, alerts);
    }
}
