package ru.donskikh.incidenthub.alertgroup.web;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.v3.oas.annotations.media.DiscriminatorMapping;
import io.swagger.v3.oas.annotations.media.Schema;
import ru.donskikh.incidenthub.alert.correlation.Evidence;

import java.util.List;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = EvidenceResponse.TemporalProximity.class, name = "TEMPORAL_PROXIMITY"),
        @JsonSubTypes.Type(value = EvidenceResponse.EarliestAlert.class, name = "EARLIEST_ALERT"),
        @JsonSubTypes.Type(value = EvidenceResponse.DependencyPath.class, name = "DEPENDENCY_PATH"),
        @JsonSubTypes.Type(value = EvidenceResponse.Migrated.class, name = "MIGRATED")
})
@Schema(oneOf = {EvidenceResponse.TemporalProximity.class, EvidenceResponse.EarliestAlert.class,
        EvidenceResponse.DependencyPath.class, EvidenceResponse.Migrated.class}, discriminatorProperty = "type",
        discriminatorMapping = {
                @DiscriminatorMapping(value = "TEMPORAL_PROXIMITY", schema = EvidenceResponse.TemporalProximity.class),
                @DiscriminatorMapping(value = "EARLIEST_ALERT", schema = EvidenceResponse.EarliestAlert.class),
                @DiscriminatorMapping(value = "DEPENDENCY_PATH", schema = EvidenceResponse.DependencyPath.class),
                @DiscriminatorMapping(value = "MIGRATED", schema = EvidenceResponse.Migrated.class)
        })
public sealed interface EvidenceResponse {
    @Schema(name = "TemporalProximityEvidence")
    record TemporalProximity(long alertId, String distanceToGroup) implements EvidenceResponse {}
    @Schema(name = "EarliestAlertEvidence")
    record EarliestAlert(long alertId, long serviceId) implements EvidenceResponse {}
    @Schema(name = "DependencyPathEvidence")
    record DependencyPath(List<Long> serviceIds) implements EvidenceResponse {}
    @Schema(name = "MigratedEvidence")
    record Migrated() implements EvidenceResponse {}

    static EvidenceResponse from(Evidence evidence) {
        return switch (evidence) {
            case Evidence.TemporalProximity temporal -> new TemporalProximity(temporal.alertId(), temporal.distanceToGroup().toString());
            case Evidence.EarliestAlert earliest -> new EarliestAlert(earliest.alertId(), earliest.serviceId());
            case Evidence.DependencyPath path -> new DependencyPath(path.serviceIds());
            case Evidence.Migrated ignored -> new Migrated();
        };
    }
}
