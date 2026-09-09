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
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import ru.donskikh.incidenthub.team.Team;

import java.time.Instant;
import java.util.Locale;
import java.util.Objects;

@Entity
@Table(name = "business_services")
public class BusinessService {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "code", nullable = false, length = 50)
    private String code;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "description", nullable = false, columnDefinition = "text")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_team_id", nullable = false)
    private Team ownerTeam;

    @Enumerated(EnumType.STRING)
    @Column(name = "tier", nullable = false, length = 20)
    private ServiceTier tier;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected BusinessService() {
    }

    public BusinessService(
            String code,
            String name,
            String description,
            Team ownerTeam,
            ServiceTier tier
    ) {
        this.code = normalizeCode(code);
        this.name = requireText(name, "name");
        this.description = requireText(description, "description");
        this.ownerTeam = Objects.requireNonNull(ownerTeam, "ownerTeam must not be null");
        this.tier = Objects.requireNonNull(tier, "tier must not be null");
        this.active = true;
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

    public void changeName(String name) {
        this.name = requireText(name, "name");
    }

    public void updateDescription(String description) {
        this.description = requireText(description, "description");
    }

    public void changeOwnerTeam(Team ownerTeam) {
        this.ownerTeam = Objects.requireNonNull(ownerTeam, "ownerTeam must not be null");
    }

    public void changeTier(ServiceTier tier) {
        this.tier = Objects.requireNonNull(tier, "tier must not be null");
    }

    public void activate() {
        this.active = true;
    }

    public void deactivate() {
        this.active = false;
    }

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Team getOwnerTeam() {
        return ownerTeam;
    }

    public ServiceTier getTier() {
        return tier;
    }

    public boolean isActive() {
        return active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    private static String normalizeCode(String value) {
        return requireText(value, "code").toUpperCase(Locale.ROOT);
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
