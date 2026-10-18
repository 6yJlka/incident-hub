package ru.donskikh.incidenthub.incident.application;

import ru.donskikh.incidenthub.incident.IncidentPriority;
import ru.donskikh.incidenthub.incident.IncidentSeverity;
import ru.donskikh.incidenthub.incident.IncidentSource;
import java.util.Objects;

public record CreateIncidentCommand(
        String title,
        String description,
        long affectedServiceId,
        IncidentPriority priority,
        IncidentSeverity severity,
        long actorId,
        Long responsibleTeamId,
        IncidentSource source
) {

    public CreateIncidentCommand {
        Objects.requireNonNull(source, "source must not be null");
        if (affectedServiceId <= 0) {
            throw new IllegalArgumentException("affectedServiceId must be positive");
        }

        if (actorId <= 0) {
            throw new IllegalArgumentException("actorId must be positive");
        }

        if (responsibleTeamId != null && responsibleTeamId <= 0) {
            throw new IllegalArgumentException("responsibleTeamId must be positive");
        }
    }

    public CreateIncidentCommand(String title, String description, long affectedServiceId,
            IncidentPriority priority, IncidentSeverity severity, long actorId, Long responsibleTeamId) {
        this(title, description, affectedServiceId, priority, severity, actorId, responsibleTeamId, IncidentSource.MANUAL);
    }
}
