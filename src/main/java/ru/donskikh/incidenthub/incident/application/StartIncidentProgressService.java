package ru.donskikh.incidenthub.incident.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.donskikh.incidenthub.incident.Incident;
import ru.donskikh.incidenthub.incident.IncidentNotFoundException;
import ru.donskikh.incidenthub.incident.IncidentRepository;

import java.util.Objects;

@Service
public class StartIncidentProgressService {

    private final IncidentRepository incidentRepository;

    public StartIncidentProgressService(IncidentRepository incidentRepository) {
        this.incidentRepository = incidentRepository;
    }

    @Transactional
    public StartIncidentProgressResult start(StartIncidentProgressCommand command) {
        Objects.requireNonNull(command, "command must not be null");

        Incident incident = incidentRepository.findById(command.incidentId())
                .orElseThrow(() -> new IncidentNotFoundException(command.incidentId()));

        incident.startProgress();

        return new StartIncidentProgressResult(incident.getId(), incident.getStatus());
    }
}
