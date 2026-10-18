package ru.donskikh.incidenthub.alertgroup;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import ru.donskikh.incidenthub.identity.User;
import ru.donskikh.incidenthub.catalog.BusinessService;
import ru.donskikh.incidenthub.incident.Incident;

import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "alert_groups")
public class AlertGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    @Column(nullable = false)
    private long version;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "decided_by_id")
    private User decidedBy;

    @Column(name = "decided_at")
    private Instant decidedAt;

    @Column(name = "dismiss_reason", length = 2000)
    private String dismissReason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AlertGroupStatus status;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "probable_root_service_id", nullable = false)
    private BusinessService probableRootService;

    @Column(name = "first_alert_at", nullable = false)
    private Instant firstAlertAt;

    @Column(name = "last_alert_at", nullable = false)
    private Instant lastAlertAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "incident_id")
    private Incident incident;

    protected AlertGroup() {
    }

    public AlertGroup(BusinessService probableRootService, Instant occurredAt) {
        this.probableRootService = Objects.requireNonNull(probableRootService, "probableRootService must not be null");
        this.firstAlertAt = Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        this.lastAlertAt = occurredAt;
        this.status = AlertGroupStatus.OPEN;
    }

    public void includeAlert(Instant occurredAt, BusinessService probableRootService) {
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        Objects.requireNonNull(probableRootService, "probableRootService must not be null");
        if (status != AlertGroupStatus.OPEN) {
            throw new IllegalStateException("Only open alert groups may receive alerts");
        }
        this.firstAlertAt = occurredAt.isBefore(firstAlertAt) ? occurredAt : firstAlertAt;
        this.lastAlertAt = occurredAt.isAfter(lastAlertAt) ? occurredAt : lastAlertAt;
        this.probableRootService = probableRootService;
    }

    public void requireOpen() {
        if (status != AlertGroupStatus.OPEN) {
            throw new AlertGroupStateConflictException(status);
        }
    }

    public void confirm(Incident incident, User actor, Instant decidedAt) {
        requireOpen();
        this.incident = Objects.requireNonNull(incident, "incident must not be null");
        this.decidedBy = Objects.requireNonNull(actor, "actor must not be null");
        this.decidedAt = Objects.requireNonNull(decidedAt, "decidedAt must not be null");
        this.status = AlertGroupStatus.CONFIRMED;
    }

    public void dismiss(User actor, Instant decidedAt, String reason) {
        requireOpen();
        if (reason != null && reason.length() > 2000) {
            throw new IllegalArgumentException("reason must not exceed 2000 characters");
        }
        this.decidedBy = Objects.requireNonNull(actor, "actor must not be null");
        this.decidedAt = Objects.requireNonNull(decidedAt, "decidedAt must not be null");
        this.dismissReason = reason;
        this.status = AlertGroupStatus.DISMISSED;
    }

    public long getVersion() {
        return version;
    }

    public User getDecidedBy() {
        return decidedBy;
    }

    public Instant getDecidedAt() {
        return decidedAt;
    }

    public String getDismissReason() {
        return dismissReason;
    }

    public Long getId() {
        return id;
    }

    public AlertGroupStatus getStatus() {
        return status;
    }

    public BusinessService getProbableRootService() {
        return probableRootService;
    }

    public Instant getFirstAlertAt() {
        return firstAlertAt;
    }

    public Instant getLastAlertAt() {
        return lastAlertAt;
    }

    public Incident getIncident() {
        return incident;
    }
}
