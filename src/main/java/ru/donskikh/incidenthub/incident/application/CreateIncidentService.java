package ru.donskikh.incidenthub.incident.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.donskikh.incidenthub.identity.User;
import ru.donskikh.incidenthub.identity.UserNotFoundException;
import ru.donskikh.incidenthub.identity.UserRepository;
import ru.donskikh.incidenthub.incident.Incident;
import ru.donskikh.incidenthub.incident.IncidentRepository;
import ru.donskikh.incidenthub.incident.IncidentSource;
import ru.donskikh.incidenthub.team.Team;
import ru.donskikh.incidenthub.team.TeamNotFoundException;
import ru.donskikh.incidenthub.team.TeamRepository;

import java.util.Objects;

@Service
public class CreateIncidentService {

    private final UserRepository userRepository;
    private final TeamRepository teamRepository;
    private final IncidentRepository incidentRepository;

    public CreateIncidentService(
            UserRepository userRepository,
            TeamRepository teamRepository,
            IncidentRepository incidentRepository
    ) {
        this.userRepository = userRepository;
        this.teamRepository = teamRepository;
        this.incidentRepository = incidentRepository;
    }

    @Transactional
    public CreateIncidentResult create(CreateIncidentCommand command) {
        Objects.requireNonNull(command, "command must not be null");

        User reporter = userRepository.findById(command.reporterId())
                .orElseThrow(() -> new UserNotFoundException(command.reporterId()));

        Team responsibleTeam = null;
        if (command.responsibleTeamId() != null) {
            responsibleTeam = teamRepository.findById(command.responsibleTeamId())
                    .orElseThrow(() -> new TeamNotFoundException(command.responsibleTeamId()));
        }

        Incident incident = new Incident(
                command.title(),
                command.description(),
                command.category(),
                IncidentSource.MANUAL,
                command.priority(),
                reporter,
                responsibleTeam
        );

        Incident savedIncident = incidentRepository.save(incident);

        return new CreateIncidentResult(savedIncident.getId(), savedIncident.getStatus());
    }
}
