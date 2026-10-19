package ru.donskikh.incidenthub.alertgroup.application;

import ru.donskikh.incidenthub.incident.IncidentSeverity;
import ru.donskikh.incidenthub.incident.IncidentPriority;
import java.util.Objects;

public record ConfirmAlertGroupCommand(long groupId, IncidentSeverity severity, IncidentPriority priority,
        Long businessServiceId, String title, String description, long actorId, long expectedVersion) {
    public ConfirmAlertGroupCommand {
        if (expectedVersion < 0) {
            throw new IllegalArgumentException("expectedVersion must not be negative");
        }
        if (groupId <= 0 || actorId <= 0) {
            throw new IllegalArgumentException("groupId and actorId must be positive");
        }
        Objects.requireNonNull(severity, "severity must not be null");
        Objects.requireNonNull(priority, "priority must not be null");
        if (businessServiceId != null && businessServiceId <= 0) {
            throw new IllegalArgumentException("businessServiceId must be positive");
        }
        if (title != null && (title.isBlank() || title.length() > 255)) {
            throw new IllegalArgumentException("title must contain text and not exceed 255 characters");
        }
        if (description != null && description.isBlank()) {
            throw new IllegalArgumentException("description must contain text");
        }
    }
}
