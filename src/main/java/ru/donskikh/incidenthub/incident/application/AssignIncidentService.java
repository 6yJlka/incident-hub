package ru.donskikh.incidenthub.incident.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.donskikh.incidenthub.identity.User;
import ru.donskikh.incidenthub.identity.UserNotFoundException;
import ru.donskikh.incidenthub.identity.UserRepository;
import ru.donskikh.incidenthub.incident.Incident;
import ru.donskikh.incidenthub.incident.IncidentNotFoundException;
import ru.donskikh.incidenthub.incident.IncidentRepository;

import java.util.Objects;

@Service
public class AssignIncidentService {

    private final IncidentRepository incidentRepository;
    private final UserRepository userRepository;

    public AssignIncidentService(IncidentRepository incidentRepository, UserRepository userRepository) {
        this.incidentRepository = incidentRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public AssignIncidentResult assign(AssignIncidentCommand command) {
        Objects.requireNonNull(command, "command must not be null");

        Incident incident = incidentRepository.findById(command.incidentId())
                .orElseThrow(() -> new IncidentNotFoundException(command.incidentId()));

        User assignee = userRepository.findById(command.assigneeId())
                .orElseThrow(() -> new UserNotFoundException(command.assigneeId()));

        incident.assignTo(assignee);

        return new AssignIncidentResult(
                incident.getId(),
                assignee.getId(),
                incident.getStatus()
        );
    }
}
