package ru.donskikh.incidenthub.alertgroup.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.util.ReflectionTestUtils;
import ru.donskikh.incidenthub.alert.AlertRepository;
import ru.donskikh.incidenthub.alertgroup.AlertGroup;
import ru.donskikh.incidenthub.alertgroup.AlertGroupConcurrentModificationException;
import ru.donskikh.incidenthub.alertgroup.AlertGroupNotFoundException;
import ru.donskikh.incidenthub.alertgroup.AlertGroupRepository;
import ru.donskikh.incidenthub.alertgroup.AlertGroupStateConflictException;
import ru.donskikh.incidenthub.alertgroup.AlertGroupStatus;
import ru.donskikh.incidenthub.catalog.BusinessService;
import ru.donskikh.incidenthub.catalog.BusinessServiceNotFoundException;
import ru.donskikh.incidenthub.catalog.BusinessServiceRepository;
import ru.donskikh.incidenthub.catalog.ServiceTier;
import ru.donskikh.incidenthub.identity.User;
import ru.donskikh.incidenthub.identity.UserRepository;
import ru.donskikh.incidenthub.incident.Incident;
import ru.donskikh.incidenthub.incident.IncidentPriority;
import ru.donskikh.incidenthub.incident.IncidentRepository;
import ru.donskikh.incidenthub.incident.IncidentSeverity;
import ru.donskikh.incidenthub.incident.IncidentSource;
import ru.donskikh.incidenthub.incident.IncidentStatus;
import ru.donskikh.incidenthub.incident.application.CreateIncidentCommand;
import ru.donskikh.incidenthub.incident.application.CreateIncidentResult;
import ru.donskikh.incidenthub.incident.application.CreateIncidentService;
import ru.donskikh.incidenthub.team.Team;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConfirmAlertGroupServiceTest {
    @Mock private AlertGroupRepository groups;
    @Mock private AlertRepository alerts;
    @Mock private BusinessServiceRepository services;
    @Mock private UserRepository users;
    @Mock private IncidentRepository incidents;
    @Mock private CreateIncidentService createIncident;
    @Mock private GetAlertGroupService getGroup;
    @InjectMocks private ConfirmAlertGroupService service;

    private AlertGroup group;
    private BusinessService root;
    private User actor;

    @BeforeEach
    void setUp() {
        root = businessService(10L, "Root");
        group = ru.donskikh.incidenthub.CorrelationTestData.group(root, Instant.parse("2026-10-18T12:00:00Z"));
        ReflectionTestUtils.setField(group, "id", 7L);
        actor = new User("engineer@example.com", "Engineer");
    }

    @Test
    void usesRootAndGeneratedTextAndDelegatesCreation() {
        prepare();
        when(alerts.countByGroup_Id(7)).thenReturn(3L);
        service.confirm(command(null, null, null));
        verify(createIncident).create(new CreateIncidentCommand("Monitoring alerts: Root (3)",
                "Confirmed alert group #7 for Root (SERVICE_10). Alerts: 3.", 10L,
                IncidentPriority.HIGH, IncidentSeverity.SEV2, 20L, null, IncidentSource.AUTOMATIC));
        assertThat(group.getStatus()).isEqualTo(AlertGroupStatus.CONFIRMED);
        assertThat(group.getDecidedBy()).isSameAs(actor);
        assertThat(group.getDecidedAt()).isNotNull();
        verifyNoInteractions(services);
    }

    @Test
    void usesOverrideServiceAndExplicitText() {
        prepare();
        when(services.findById(11L)).thenReturn(Optional.of(businessService(11L, "Override")));
        service.confirm(command(11L, "Chosen title", "Chosen description"));
        verify(createIncident).create(new CreateIncidentCommand("Chosen title", "Chosen description", 11L,
                IncidentPriority.HIGH, IncidentSeverity.SEV2, 20L, null, IncidentSource.AUTOMATIC));
        assertThat(group.getProbableRootService()).isSameAs(root);
    }

    @Test
    void generatesTextForOverrideService() {
        prepare();
        when(services.findById(11L)).thenReturn(Optional.of(businessService(11L, "Override")));
        service.confirm(command(11L, null, null));
        verify(createIncident).create(argThat(command -> command.title().contains("Override")
                && command.description().contains("SERVICE_11")));
    }

    @Test
    void missingGroupIsNotFound() {
        assertThatThrownBy(() -> service.confirm(command(null, null, null)))
                .isInstanceOf(AlertGroupNotFoundException.class);
        verifyNoInteractions(createIncident);
    }

    @Test
    void missingOverrideServiceIsNotFound() {
        when(groups.findById(7L)).thenReturn(Optional.of(group));
        assertThatThrownBy(() -> service.confirm(command(11L, null, null)))
                .isInstanceOf(BusinessServiceNotFoundException.class);
        verifyNoInteractions(createIncident);
    }

    @ParameterizedTest
    @EnumSource(value = AlertGroupStatus.class, names = {"CONFIRMED", "DISMISSED"})
    void refusesAlreadyDecidedGroup(AlertGroupStatus status) {
        ReflectionTestUtils.setField(group, "status", status);
        when(groups.findById(7L)).thenReturn(Optional.of(group));
        assertThatThrownBy(() -> service.confirm(command(null, null, null)))
                .isInstanceOf(AlertGroupStateConflictException.class);
        verifyNoInteractions(createIncident);
    }

    @Test
    void translatesVersionFailureAndDoesNotReturnCard() {
        prepareCreation();
        doThrow(new ObjectOptimisticLockingFailureException(AlertGroup.class, 7L)).when(groups).flush();
        assertThatThrownBy(() -> service.confirm(command(null, null, null)))
                .isInstanceOf(AlertGroupConcurrentModificationException.class);
        verifyNoInteractions(getGroup);
    }

    @Test
    void rejectsNullCommand() {
        assertThatNullPointerException().isThrownBy(() -> service.confirm(null));
    }

    @Test
    void refusesStaleVersionBeforeLoadingActorOrCreatingIncident() {
        when(groups.findById(7L)).thenReturn(Optional.of(group));
        ReflectionTestUtils.setField(group, "version", 2L);
        assertThatThrownBy(() -> service.confirm(command(null, null, null)))
                .isInstanceOf(AlertGroupConcurrentModificationException.class);
        org.mockito.Mockito.verifyNoInteractions(users, createIncident, getGroup, alerts, services, incidents);
    }

    private void prepare() {
        prepareCreation();
        when(getGroup.get(new GetAlertGroupQuery(7))).thenReturn(new GetAlertGroupResult(
                AlertGroupItem.from(group), List.of(), null, null, null, null, 0, group.getRootAssessment()));
    }

    private void prepareCreation() {
        when(groups.findById(7L)).thenReturn(Optional.of(group));
        when(users.findById(20L)).thenReturn(Optional.of(actor));
        when(createIncident.create(any())).thenReturn(new CreateIncidentResult(42L, IncidentStatus.OPEN));
        when(incidents.getReferenceById(42L)).thenReturn(mock(Incident.class));
    }

    private ConfirmAlertGroupCommand command(Long serviceId, String title, String description) {
        return new ConfirmAlertGroupCommand(7, IncidentSeverity.SEV2, IncidentPriority.HIGH,
                serviceId, title, description, 20, 0);
    }

    private BusinessService businessService(long id, String name) {
        var service = new BusinessService("SERVICE_" + id, name, "Description", new Team("Team", "TEAM"), ServiceTier.TIER_1);
        ReflectionTestUtils.setField(service, "id", id);
        return service;
    }
}
