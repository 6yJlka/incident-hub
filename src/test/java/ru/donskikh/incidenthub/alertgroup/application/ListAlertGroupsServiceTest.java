package ru.donskikh.incidenthub.alertgroup.application;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import ru.donskikh.incidenthub.alertgroup.AlertGroup;
import ru.donskikh.incidenthub.alertgroup.AlertGroupRepository;
import ru.donskikh.incidenthub.catalog.BusinessService;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ListAlertGroupsServiceTest {

    @Test
    void mapsGroupsAndOrdersByLastAlertThenId() {
        AlertGroupRepository repository = mock(AlertGroupRepository.class);
        BusinessService root = mock(BusinessService.class);
        when(root.getId()).thenReturn(42L);
        when(root.getCode()).thenReturn("ROOT");
        when(root.getName()).thenReturn("Root");
        AlertGroup group = new AlertGroup(root, Instant.parse("2026-10-18T12:00:00Z"));
        org.springframework.test.util.ReflectionTestUtils.setField(group, "id", 7L);
        when(repository.findAll(ArgumentMatchers.<Specification<AlertGroup>>any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(group)));

        var result = new ListAlertGroupsService(repository).execute(new ListAlertGroupsQuery(0, 20, null, 42L));

        assertThat(result.items()).singleElement().satisfies(item -> {
            assertThat(item.id()).isEqualTo(7);
            assertThat(item.probableRootServiceCode()).isEqualTo("ROOT");
        });
        var pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(repository).findAll(ArgumentMatchers.<Specification<AlertGroup>>any(), pageable.capture());
        assertThat(pageable.getValue().getSort().stream().map(order -> order.getProperty()).toList())
                .containsExactly("lastAlertAt", "id");
        assertThat(pageable.getValue().getSort()).allSatisfy(order -> assertThat(order.isDescending()).isTrue());
    }

    @Test
    void rejectsNullQueryWithoutDatabaseAccess() {
        var repository = mock(AlertGroupRepository.class);
        assertThatThrownBy(() -> new ListAlertGroupsService(repository).execute(null))
                .isInstanceOf(NullPointerException.class);
        verifyNoInteractions(repository);
    }
}
