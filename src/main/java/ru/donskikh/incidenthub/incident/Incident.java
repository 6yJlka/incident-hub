package ru.donskikh.incidenthub.incident;

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
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import ru.donskikh.incidenthub.catalog.BusinessService;
import ru.donskikh.incidenthub.identity.User;
import ru.donskikh.incidenthub.team.Team;

import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "incidents")
public class Incident {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "description", nullable = false, columnDefinition = "text")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "affected_service_id", nullable = false)
    private BusinessService affectedService;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 20)
    private IncidentSource source;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 20)
    private IncidentPriority priority;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 10)
    private IncidentSeverity severity;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private IncidentStatus status;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reporter_id", nullable = false)
    private User reporter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "responsible_team_id")
    private Team responsibleTeam;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignee_id")
    private User assignee;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Incident() {
    }

    public Incident(
            String title,
            String description,
            BusinessService affectedService,
            IncidentSource source,
            IncidentPriority priority,
            IncidentSeverity severity,
            User reporter,
            Team responsibleTeam
    ) {
        this.title = requireText(title, "title");
        this.description = requireText(description, "description");
        this.affectedService = Objects.requireNonNull(affectedService, "affectedService must not be null");
        this.source = Objects.requireNonNull(source, "source must not be null");
        this.priority = Objects.requireNonNull(priority, "priority must not be null");
        this.severity = Objects.requireNonNull(severity, "severity must not be null");
        this.status = IncidentStatus.OPEN;
        this.reporter = Objects.requireNonNull(reporter, "reporter must not be null");
        this.responsibleTeam = responsibleTeam;
    }

    @PrePersist
    private void prePersist() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    private void preUpdate() {
        this.updatedAt = Instant.now();
    }

    public void changeTitle(String title) {
        this.title = requireText(title, "title");
    }

    public void updateDescription(String description) {
        this.description = requireText(description, "description");
    }

    public void changeAffectedService(BusinessService affectedService) {
        this.affectedService = Objects.requireNonNull(affectedService, "affectedService must not be null");
    }

    public void changePriority(IncidentPriority priority) {
        this.priority = Objects.requireNonNull(priority, "priority must not be null");
    }

    public void changeSeverity(IncidentSeverity severity) {
        this.severity = Objects.requireNonNull(severity, "severity must not be null");
    }

    public void assignTo(User assignee) {
        Objects.requireNonNull(assignee, "assignee must not be null");

        if (!status.allowsAssignment()) {
            throw new IncidentAssignmentNotAllowedException(id, status);
        }

        this.assignee = assignee;
        this.status = IncidentStatus.ASSIGNED;
    }

    public void startProgress() {
        if (!status.allowsStartProgress()) {
            throw new IncidentStartProgressNotAllowedException(id, status);
        }

        this.status = IncidentStatus.IN_PROGRESS;
    }

    public void resolve() {
        if (!status.allowsResolve()) {
            throw new IncidentResolutionNotAllowedException(id, status);
        }

        this.status = IncidentStatus.RESOLVED;
    }

    public void close() {
        if (!status.allowsClose()) {
            throw new IncidentClosureNotAllowedException(id, status);
        }

        this.status = IncidentStatus.CLOSED;
    }

    public void reopen() {
        if (!status.allowsReopen()) {
            throw new IncidentReopenNotAllowedException(id, status);
        }

        this.status = IncidentStatus.IN_PROGRESS;
    }

    public void cancel() {
        if (!status.allowsCancellation()) {
            throw new IncidentCancellationNotAllowedException(id, status);
        }

        this.status = IncidentStatus.CANCELLED;
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public BusinessService getAffectedService() {
        return affectedService;
    }

    public IncidentSource getSource() {
        return source;
    }

    public IncidentPriority getPriority() {
        return priority;
    }

    public IncidentSeverity getSeverity() {
        return severity;
    }

    public IncidentStatus getStatus() {
        return status;
    }

    public User getReporter() {
        return reporter;
    }

    public Team getResponsibleTeam() {
        return responsibleTeam;
    }

    public User getAssignee() {
        return assignee;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    private static String requireText(String value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }

        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }

        return trimmed;
    }
}
