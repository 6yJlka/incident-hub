package ru.donskikh.incidenthub.incident.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.donskikh.incidenthub.incident.Incident;
import ru.donskikh.incidenthub.incident.IncidentNotFoundException;
import ru.donskikh.incidenthub.incident.IncidentRepository;

import java.util.Objects;

@Service
public class ResolveIncidentService {

    private final IncidentRepository incidentRepository;

    public ResolveIncidentService(IncidentRepository incidentRepository) {
        this.incidentRepository = incidentRepository;
    }

    @Transactional
    public ResolveIncidentResult resolve(ResolveIncidentCommand command) {
        Objects.requireNonNull(command, "command must not be null");

        Incident incident = incidentRepository.findById(command.incidentId())
                .orElseThrow(() -> new IncidentNotFoundException(command.incidentId()));

        incident.resolve();

        return new ResolveIncidentResult(incident.getId(), incident.getStatus());
    }
}
