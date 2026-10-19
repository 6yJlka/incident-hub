package ru.donskikh.incidenthub.alertgroup.application;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.util.ReflectionTestUtils;
import ru.donskikh.incidenthub.alertgroup.AlertGroup;
import ru.donskikh.incidenthub.alertgroup.AlertGroupRepository;
import ru.donskikh.incidenthub.alertgroup.AlertGroupStatus;
import ru.donskikh.incidenthub.alertgroup.AlertGroupNotFoundException;
import ru.donskikh.incidenthub.alertgroup.AlertGroupStateConflictException;
import ru.donskikh.incidenthub.alertgroup.AlertGroupConcurrentModificationException;
import ru.donskikh.incidenthub.catalog.BusinessService;
import ru.donskikh.incidenthub.identity.User;
import ru.donskikh.incidenthub.identity.UserRepository;
import java.time.Instant;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DismissAlertGroupServiceTest {
    @Mock private AlertGroupRepository groups;
    @Mock private UserRepository users;
    @Mock private GetAlertGroupService getGroup;
    @InjectMocks private DismissAlertGroupService service;

    @Test
    void recordsDecisionWithoutIncident() {
        var group = group();
        var actor = new User("engineer@example.com", "Engineer");
        when(groups.findById(7L)).thenReturn(Optional.of(group));
        when(users.findById(20L)).thenReturn(Optional.of(actor));
        service.dismiss(new DismissAlertGroupCommand(7, "False positive", 20, 0));
        assertThat(group.getStatus()).isEqualTo(AlertGroupStatus.DISMISSED);
        assertThat(group.getIncident()).isNull();
        assertThat(group.getDecidedBy()).isSameAs(actor);
        assertThat(group.getDecidedAt()).isNotNull();
        assertThat(group.getDismissReason()).isEqualTo("False positive");
        verify(groups).flush();
        verify(getGroup).get(new GetAlertGroupQuery(7));
    }

    @Test
    void missingGroupIsNotFound() {
        assertThatThrownBy(() -> service.dismiss(new DismissAlertGroupCommand(7, null, 20, 0)))
                .isInstanceOf(AlertGroupNotFoundException.class);
    }

    @ParameterizedTest
    @EnumSource(value = AlertGroupStatus.class, names = {"CONFIRMED", "DISMISSED"})
    void refusesAlreadyDecidedGroup(AlertGroupStatus status) {
        var group = group();
        ReflectionTestUtils.setField(group, "status", status);
        when(groups.findById(7L)).thenReturn(Optional.of(group));
        assertThatThrownBy(() -> service.dismiss(new DismissAlertGroupCommand(7, null, 20, 0)))
                .isInstanceOf(AlertGroupStateConflictException.class);
        verifyNoInteractions(users, getGroup);
    }

    @Test
    void translatesVersionFailure() {
        when(groups.findById(7L)).thenReturn(Optional.of(group()));
        when(users.findById(20L)).thenReturn(Optional.of(new User("engineer@example.com", "Engineer")));
        doThrow(new ObjectOptimisticLockingFailureException(AlertGroup.class, 7L)).when(groups).flush();
        assertThatThrownBy(() -> service.dismiss(new DismissAlertGroupCommand(7, null, 20, 0)))
                .isInstanceOf(AlertGroupConcurrentModificationException.class);
        verifyNoInteractions(getGroup);
    }

    @Test
    void rejectsNullCommand() {
        assertThatNullPointerException().isThrownBy(() -> service.dismiss(null));
    }

    @Test
    void refusesStaleVersionBeforeLoadingActor() {
        var group = group();
        ReflectionTestUtils.setField(group, "version", 2L);
        when(groups.findById(7L)).thenReturn(Optional.of(group));
        assertThatThrownBy(() -> service.dismiss(new DismissAlertGroupCommand(7, null, 20, 0)))
                .isInstanceOf(AlertGroupConcurrentModificationException.class);
        verifyNoInteractions(users, getGroup);
    }

    private AlertGroup group() {
        var group = new AlertGroup(mock(BusinessService.class), Instant.now());
        ReflectionTestUtils.setField(group, "id", 7L);
        return group;
    }
}
