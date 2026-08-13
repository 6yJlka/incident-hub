package ru.donskikh.incidenthub.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;
import ru.donskikh.incidenthub.incident.IncidentStatus;

import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "incident_audit_events")
@Immutable
public class IncidentAuditEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "incident_id", nullable = false, updatable = false)
    private Long incidentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, updatable = false, length = 30)
    private IncidentAuditEventType eventType;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", updatable = false, length = 30)
    private IncidentStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false, updatable = false, length = 30)
    private IncidentStatus toStatus;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected IncidentAuditEvent() {
    }

    public IncidentAuditEvent(
            long incidentId,
            IncidentAuditEventType eventType,
            IncidentStatus fromStatus,
            IncidentStatus toStatus
    ) {
        if (incidentId <= 0) {
            throw new IllegalArgumentException("incidentId must be positive");
        }

        this.incidentId = incidentId;
        this.eventType = Objects.requireNonNull(eventType, "eventType must not be null");
        this.fromStatus = fromStatus;
        this.toStatus = Objects.requireNonNull(toStatus, "toStatus must not be null");
    }

    @PrePersist
    private void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public Long getId() {
        return id;
    }

    public Long getIncidentId() {
        return incidentId;
    }

    public IncidentAuditEventType getEventType() {
        return eventType;
    }

    public IncidentStatus getFromStatus() {
        return fromStatus;
    }

    public IncidentStatus getToStatus() {
        return toStatus;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
