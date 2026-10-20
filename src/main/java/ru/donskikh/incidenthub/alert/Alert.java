package ru.donskikh.incidenthub.alert;

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
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;
import ru.donskikh.incidenthub.alert.correlation.Evidence;
import ru.donskikh.incidenthub.alert.correlation.LinkStrength;
import ru.donskikh.incidenthub.alert.infrastructure.LinkEvidenceConverter;
import ru.donskikh.incidenthub.alertgroup.AlertGroup;
import ru.donskikh.incidenthub.catalog.BusinessService;

import java.time.Instant;
import java.util.List;

@Entity
@Table(name = "alerts")
@Immutable
public class Alert {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, updatable = false, length = 100)
    private String source;

    @Column(name = "external_id", nullable = false, updatable = false, length = 255)
    private String externalId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "business_service_id", nullable = false, updatable = false)
    private BusinessService businessService;

    @Enumerated(EnumType.STRING)
    @Column(name = "signal_type", nullable = false, updatable = false, length = 20)
    private AlertSignalType signalType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 10)
    private AlertSeverity severity;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    @Column(name = "received_at", nullable = false, updatable = false)
    private Instant receivedAt;

    @Column(nullable = false, updatable = false, length = 2000)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", updatable = false)
    private AlertGroup group;

    @Enumerated(EnumType.STRING)
    @Column(name = "link_strength", updatable = false, length = 20)
    private LinkStrength linkStrength;

    @Convert(converter = LinkEvidenceConverter.class)
    @Column(name = "link_evidence", updatable = false, columnDefinition = "jsonb")
    private List<Evidence> linkEvidence;

    public LinkStrength getLinkStrength() { return linkStrength; }
    public List<Evidence> getLinkEvidence() { return linkEvidence; }

    public AlertGroup getGroup() {
        return group;
    }

    protected Alert() {
    }

    public Long getId() {
        return id;
    }

    public String getSource() {
        return source;
    }

    public String getExternalId() {
        return externalId;
    }

    public BusinessService getBusinessService() {
        return businessService;
    }

    public AlertSignalType getSignalType() {
        return signalType;
    }

    public AlertSeverity getSeverity() {
        return severity;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public Instant getReceivedAt() {
        return receivedAt;
    }

    public String getDescription() {
        return description;
    }

}
