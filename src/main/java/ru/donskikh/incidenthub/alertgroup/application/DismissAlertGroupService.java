package ru.donskikh.incidenthub.alertgroup.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import ru.donskikh.incidenthub.alertgroup.AlertGroupRepository;
import ru.donskikh.incidenthub.alertgroup.AlertGroupNotFoundException;
import ru.donskikh.incidenthub.alertgroup.AlertGroupConcurrentModificationException;
import ru.donskikh.incidenthub.identity.UserRepository;
import ru.donskikh.incidenthub.identity.UserNotFoundException;
import java.time.Instant;
import java.util.Objects;

@Service
public class DismissAlertGroupService {
    private final AlertGroupRepository groups;
    private final UserRepository users;
    private final GetAlertGroupService getGroup;

    public DismissAlertGroupService(AlertGroupRepository groups, UserRepository users, GetAlertGroupService getGroup) {
        this.groups = groups;
        this.users = users;
        this.getGroup = getGroup;
    }

    @Transactional
    public GetAlertGroupResult dismiss(DismissAlertGroupCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        var group = groups.findById(command.groupId())
                .orElseThrow(() -> new AlertGroupNotFoundException(command.groupId()));
        group.requireOpen();
        var actor = users.findById(command.actorId())
                .orElseThrow(() -> new UserNotFoundException(command.actorId()));
        group.dismiss(actor, Instant.now(), command.reason());
        try {
            groups.flush();
        } catch (ObjectOptimisticLockingFailureException exception) {
            throw new AlertGroupConcurrentModificationException(command.groupId(), exception);
        }
        return getGroup.get(new GetAlertGroupQuery(group.getId()));
    }
}
