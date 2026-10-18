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
