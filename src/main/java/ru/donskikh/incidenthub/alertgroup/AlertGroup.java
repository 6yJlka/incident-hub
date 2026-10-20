package ru.donskikh.incidenthub.alertgroup;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.ColumnTransformer;
import ru.donskikh.incidenthub.alert.correlation.RootAssessment;
import ru.donskikh.incidenthub.alert.correlation.RootAssessmentStatus;
import ru.donskikh.incidenthub.alert.infrastructure.RootAssessmentConverter;
import ru.donskikh.incidenthub.catalog.BusinessService;
import ru.donskikh.incidenthub.identity.User;
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "probable_root_service_id")
    private BusinessService probableRootService;

    @Column(name = "first_alert_at", nullable = false)
    private Instant firstAlertAt;

    @Column(name = "last_alert_at", nullable = false)
    private Instant lastAlertAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "incident_id")
    private Incident incident;

    @Convert(converter = RootAssessmentConverter.class)
    @ColumnTransformer(write = "?::jsonb")
    @Column(name = "root_assessment", nullable = false, columnDefinition = "jsonb")
    private RootAssessment rootAssessment;

    @Enumerated(EnumType.STRING)
    @Column(name = "root_assessment_status", nullable = false, length = 20)
    private RootAssessmentStatus rootAssessmentStatus;

    protected AlertGroup() {
    }

    public AlertGroup(BusinessService probableRootService, Instant occurredAt, RootAssessment rootAssessment) {
        setRootAssessment(rootAssessment, probableRootService);
        this.firstAlertAt = Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        this.lastAlertAt = occurredAt;
        this.status = AlertGroupStatus.OPEN;
    }

    public void includeAlert(Instant occurredAt, BusinessService probableRootService, RootAssessment rootAssessment) {
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        if (status != AlertGroupStatus.OPEN) {
            throw new IllegalStateException("Only open alert groups may receive alerts");
        }
        validateRoot(rootAssessment, probableRootService);
        this.firstAlertAt = occurredAt.isBefore(firstAlertAt) ? occurredAt : firstAlertAt;
        this.lastAlertAt = occurredAt.isAfter(lastAlertAt) ? occurredAt : lastAlertAt;
        setRootAssessment(rootAssessment, probableRootService);
    }

    private void setRootAssessment(RootAssessment assessment, BusinessService service) {
        Objects.requireNonNull(assessment, "rootAssessment must not be null");
        validateRoot(assessment, service);
        this.rootAssessment = assessment;
        this.rootAssessmentStatus = RootAssessmentStatus.of(assessment);
        this.probableRootService = service;
    }

    private static void validateRoot(RootAssessment assessment, BusinessService service) {
        Objects.requireNonNull(assessment, "rootAssessment must not be null");
        if (assessment instanceof RootAssessment.Ranked ranked) {
            if (service == null || !Objects.equals(service.getId(), ranked.candidates().getFirst().serviceId())) {
                throw new IllegalArgumentException("Probable root must match the first ranked candidate");
            }
        } else if (service != null) {
            throw new IllegalArgumentException("Undetermined assessment must not have a probable root");
        }
    }

    @PostLoad
    @PrePersist
    @PreUpdate
    private void validateAssessment() {
        Objects.requireNonNull(rootAssessment, "rootAssessment must not be null");
        validateRoot(rootAssessment, probableRootService);
        if (rootAssessmentStatus != RootAssessmentStatus.of(rootAssessment)) {
            throw new IllegalArgumentException("Root assessment status does not match its JSON");
        }
    }

    public RootAssessment getRootAssessment() {
        return rootAssessment;
    }

    public RootAssessmentStatus getRootAssessmentStatus() {
        return rootAssessmentStatus;
    }

    public BusinessService resolveIncidentService(BusinessService requestedService) {
        if (requestedService != null) {
            return requestedService;
        }
        if (rootAssessment instanceof RootAssessment.Undetermined) {
            throw new RootServiceRequiredException();
        }
        return probableRootService;
    }

    public void requireVersion(long expectedVersion) {
        if (version != expectedVersion) {
            throw new AlertGroupConcurrentModificationException(id);
        }
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
