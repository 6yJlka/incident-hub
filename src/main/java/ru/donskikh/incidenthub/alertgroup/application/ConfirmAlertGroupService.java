package ru.donskikh.incidenthub.alertgroup.application;

import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.donskikh.incidenthub.alert.AlertRepository;
import ru.donskikh.incidenthub.alertgroup.AlertGroupConcurrentModificationException;
import ru.donskikh.incidenthub.alertgroup.AlertGroupNotFoundException;
import ru.donskikh.incidenthub.alertgroup.AlertGroupRepository;
import ru.donskikh.incidenthub.catalog.BusinessServiceNotFoundException;
import ru.donskikh.incidenthub.catalog.BusinessServiceRepository;
import ru.donskikh.incidenthub.identity.UserNotFoundException;
import ru.donskikh.incidenthub.identity.UserRepository;
import ru.donskikh.incidenthub.incident.IncidentRepository;
import ru.donskikh.incidenthub.incident.IncidentSource;
import ru.donskikh.incidenthub.incident.application.CreateIncidentCommand;
import ru.donskikh.incidenthub.incident.application.CreateIncidentService;

import java.time.Instant;
import java.util.Objects;

@Service
public class ConfirmAlertGroupService {
    private final AlertGroupRepository groups;
    private final AlertRepository alerts;
    private final BusinessServiceRepository services;
    private final UserRepository users;
    private final IncidentRepository incidents;
    private final CreateIncidentService createIncident;
    private final GetAlertGroupService getGroup;

    public ConfirmAlertGroupService(AlertGroupRepository groups, AlertRepository alerts,
            BusinessServiceRepository services, UserRepository users, IncidentRepository incidents,
            CreateIncidentService createIncident, GetAlertGroupService getGroup) {
        this.groups = groups;
        this.alerts = alerts;
        this.services = services;
        this.users = users;
        this.incidents = incidents;
        this.createIncident = createIncident;
        this.getGroup = getGroup;
    }

    @Transactional
    public GetAlertGroupResult confirm(ConfirmAlertGroupCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        var group = groups.findById(command.groupId())
                .orElseThrow(() -> new AlertGroupNotFoundException(command.groupId()));
        group.requireVersion(command.expectedVersion());
        group.requireOpen();
        var service = group.resolveIncidentService(command.businessServiceId() == null ? null
                : services.findById(command.businessServiceId())
                    .orElseThrow(() -> new BusinessServiceNotFoundException(command.businessServiceId())));
        var actor = users.findById(command.actorId())
                .orElseThrow(() -> new UserNotFoundException(command.actorId()));
        long alertCount = alerts.countByGroup_Id(group.getId());
        String title = command.title() == null
                ? "Monitoring alerts: " + service.getName() + " (" + alertCount + ")" : command.title();
        String description = command.description() == null
                ? "Confirmed alert group #" + group.getId() + " for " + service.getName()
                    + " (" + service.getCode() + "). Alerts: " + alertCount + "." : command.description();
        try {
            var created = createIncident.create(new CreateIncidentCommand(title, description, service.getId(),
                    command.priority(), command.severity(), command.actorId(), null, IncidentSource.AUTOMATIC));
            group.confirm(incidents.getReferenceById(created.incidentId()), actor, Instant.now());
            groups.flush();
        } catch (ObjectOptimisticLockingFailureException exception) {
            throw new AlertGroupConcurrentModificationException(command.groupId(), exception);
        }
        return getGroup.get(new GetAlertGroupQuery(group.getId()));
    }
}
