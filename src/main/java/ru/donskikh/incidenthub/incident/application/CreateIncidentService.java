package ru.donskikh.incidenthub.incident.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.donskikh.incidenthub.audit.IncidentAuditEventType;
import ru.donskikh.incidenthub.audit.IncidentAuditService;
import ru.donskikh.incidenthub.catalog.BusinessService;
import ru.donskikh.incidenthub.catalog.BusinessServiceNotFoundException;
import ru.donskikh.incidenthub.catalog.BusinessServiceRepository;
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
    private final BusinessServiceRepository businessServiceRepository;
    private final TeamRepository teamRepository;
    private final IncidentRepository incidentRepository;
    private final IncidentAuditService auditService;

    public CreateIncidentService(
            UserRepository userRepository,
            BusinessServiceRepository businessServiceRepository,
            TeamRepository teamRepository,
            IncidentRepository incidentRepository,
            IncidentAuditService auditService
    ) {
        this.userRepository = userRepository;
        this.businessServiceRepository = businessServiceRepository;
        this.teamRepository = teamRepository;
        this.incidentRepository = incidentRepository;
        this.auditService = auditService;
    }

    @Transactional
    public CreateIncidentResult create(CreateIncidentCommand command) {
        Objects.requireNonNull(command, "command must not be null");

        User reporter = userRepository.findById(command.reporterId())
                .orElseThrow(() -> new UserNotFoundException(command.reporterId()));

        BusinessService affectedService = businessServiceRepository.findById(command.affectedServiceId())
                .orElseThrow(() -> new BusinessServiceNotFoundException(command.affectedServiceId()));

        Team responsibleTeam = affectedService.getOwnerTeam();
        if (command.responsibleTeamId() != null) {
            responsibleTeam = teamRepository.findById(command.responsibleTeamId())
                    .orElseThrow(() -> new TeamNotFoundException(command.responsibleTeamId()));
        }

        Incident incident = new Incident(
                command.title(),
                command.description(),
                affectedService,
                IncidentSource.MANUAL,
                command.priority(),
                command.severity(),
                reporter,
                responsibleTeam
        );

        Incident savedIncident = incidentRepository.save(incident);

        auditService.record(
                savedIncident.getId(),
                IncidentAuditEventType.CREATED,
                null,
                savedIncident.getStatus()
        );

        return new CreateIncidentResult(savedIncident.getId(), savedIncident.getStatus());
    }
}
