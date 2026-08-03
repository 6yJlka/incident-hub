package ru.donskikh.incidenthub.incident.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.donskikh.incidenthub.identity.User;
import ru.donskikh.incidenthub.identity.UserNotFoundException;
import ru.donskikh.incidenthub.identity.UserRepository;
import ru.donskikh.incidenthub.incident.Incident;
import ru.donskikh.incidenthub.incident.IncidentRepository;

import java.util.Objects;

@Service
public class CreateIncidentService {

    private final UserRepository userRepository;
    private final IncidentRepository incidentRepository;

    public CreateIncidentService(UserRepository userRepository, IncidentRepository incidentRepository) {
        this.userRepository = userRepository;
        this.incidentRepository = incidentRepository;
    }

    @Transactional
    public CreateIncidentResult create(CreateIncidentCommand command) {
        Objects.requireNonNull(command, "command must not be null");

        User reporter = userRepository.findById(command.reporterId())
                .orElseThrow(() -> new UserNotFoundException(command.reporterId()));

        Incident incident = new Incident(
                command.title(),
                command.description(),
                command.category(),
                command.priority(),
                reporter
        );

        Incident savedIncident = incidentRepository.save(incident);

        return new CreateIncidentResult(savedIncident.getId(), savedIncident.getStatus());
    }
}
