package ru.donskikh.incidenthub.catalog;

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
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.Objects;

@Entity
@Table(
        name = "service_dependencies",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_service_dependencies_pair",
                columnNames = {"dependent_id", "dependency_id"}
        )
)
public class ServiceDependency {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dependent_id", nullable = false)
    private BusinessService dependent;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dependency_id", nullable = false)
    private BusinessService dependency;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private DependencyType type;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected ServiceDependency() {
    }

    public ServiceDependency(
            BusinessService dependent,
            BusinessService dependency,
            DependencyType type
    ) {
        this.dependent = Objects.requireNonNull(dependent, "dependent must not be null");
        this.dependency = Objects.requireNonNull(dependency, "dependency must not be null");
        this.type = Objects.requireNonNull(type, "type must not be null");

        if (isSameService(dependent, dependency)) {
            throw new ServiceSelfDependencyNotAllowedException(dependent.getId());
        }
    }

    @PrePersist
    private void prePersist() {
        this.createdAt = Instant.now();
    }

    public void changeType(DependencyType type) {
        this.type = Objects.requireNonNull(type, "type must not be null");
    }

    public Long getId() {
        return id;
    }

    public BusinessService getDependent() {
        return dependent;
    }

    public BusinessService getDependency() {
        return dependency;
    }

    public DependencyType getType() {
        return type;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    private static boolean isSameService(BusinessService dependent, BusinessService dependency) {
        if (dependent == dependency) {
            return true;
        }

        return dependent.getId() != null && dependent.getId().equals(dependency.getId());
    }
}
