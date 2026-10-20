package ru.donskikh.incidenthub.alert.infrastructure;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import ru.donskikh.incidenthub.alert.correlation.Evidence;
import ru.donskikh.incidenthub.alert.correlation.RootAssessment;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

public final class CorrelationJsonCodec {
    private static final JsonMapper MAPPER = JsonMapper.builder()
            .addMixIn(RootAssessment.class, AssessmentTypes.class)
            .addMixIn(Evidence.class, EvidenceTypes.class)
            .addMixIn(Evidence.TemporalProximity.class, TemporalFormat.class)
            .enable(DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES)
            .enable(DeserializationFeature.FAIL_ON_NULL_CREATOR_PROPERTIES)
            .enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
            .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).build();

    private CorrelationJsonCodec() {}

    public static String writeAssessment(RootAssessment assessment) {
        return MAPPER.writeValueAsString(new AssessmentDocument(1, assessment));
    }

    public static RootAssessment readAssessment(String json) {
        return MAPPER.readValue(json, AssessmentDocument.class).assessment();
    }

    public static String writeEvidence(List<Evidence> evidence) {
        return MAPPER.writeValueAsString(new EvidenceDocument(1, evidence));
    }

    public static List<Evidence> readEvidence(String json) {
        return MAPPER.readValue(json, EvidenceDocument.class).items();
    }

    public record AssessmentDocument(int formatVersion, RootAssessment assessment) {
        public AssessmentDocument {
            requireVersion(formatVersion);
            Objects.requireNonNull(assessment, "assessment must not be null");
        }
    }

    public record EvidenceDocument(int formatVersion, List<Evidence> items) {
        public EvidenceDocument {
            requireVersion(formatVersion);
            items = Evidence.requireEvidence(items);
        }
    }

    private static void requireVersion(int version) {
        if (version != 1) throw new IllegalArgumentException("Unsupported correlation JSON format version: " + version);
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "status")
    @JsonSubTypes({
            @JsonSubTypes.Type(value = RootAssessment.Ranked.class, name = "RANKED"),
            @JsonSubTypes.Type(value = RootAssessment.Undetermined.class, name = "UNDETERMINED")
    })
    private interface AssessmentTypes {}

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
    @JsonSubTypes({
            @JsonSubTypes.Type(value = Evidence.TemporalProximity.class, name = "TEMPORAL_PROXIMITY"),
            @JsonSubTypes.Type(value = Evidence.EarliestAlert.class, name = "EARLIEST_ALERT"),
            @JsonSubTypes.Type(value = Evidence.DependencyPath.class, name = "DEPENDENCY_PATH"),
            @JsonSubTypes.Type(value = Evidence.Migrated.class, name = "MIGRATED")
    })
    private interface EvidenceTypes {}

    private interface TemporalFormat {
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        Duration distanceToGroup();
    }
}
