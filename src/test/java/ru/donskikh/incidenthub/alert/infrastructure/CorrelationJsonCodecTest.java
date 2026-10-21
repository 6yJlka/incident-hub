package ru.donskikh.incidenthub.alert.infrastructure;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import ru.donskikh.incidenthub.alert.correlation.*;

import java.time.Duration;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CorrelationJsonCodecTest {
    static Stream<Evidence> evidence() {
        return Stream.of(new Evidence.TemporalProximity(12, Duration.ofSeconds(-20)),
                new Evidence.EarliestAlert(12, 42), new Evidence.DependencyPath(List.of(42L, 43L)), new Evidence.Migrated(), new Evidence.PageRankScore(42, .75, 3, .85, 50, 1e-13));
    }

    @ParameterizedTest
    @MethodSource("evidence")
    void roundTripsEveryEvidenceVariantWithStableDiscriminator(Evidence evidence) {
        String json = CorrelationJsonCodec.writeEvidence(List.of(evidence));
        assertThat(json).contains("\"type\":").doesNotContain("ru.donskikh", "@class");
        assertThat(CorrelationJsonCodec.readEvidence(json)).containsExactly(evidence);
        assertThat(new LinkEvidenceConverter().convertToEntityAttribute(
                new LinkEvidenceConverter().convertToDatabaseColumn(List.of(evidence)))).containsExactly(evidence);
    }

    @Test
    void roundTripsBothAssessmentsIncludingMultipleCandidatesAndEvidence() {
        for (RootAssessment assessment : List.of(new RootAssessment.Ranked(
                List.of(new RootCandidate(42, .9), new RootCandidate(43, .3)), evidence().toList()),
                new RootAssessment.Undetermined("INSUFFICIENT_EVIDENCE"))) {
            String json = CorrelationJsonCodec.writeAssessment(assessment);
            assertThat(json).contains("\"formatVersion\":1", "\"status\":").doesNotContain("ru.donskikh");
            assertThat(CorrelationJsonCodec.readAssessment(json)).isEqualTo(assessment);
            var converter = new RootAssessmentConverter();
            assertThat(converter.convertToEntityAttribute(converter.convertToDatabaseColumn(assessment))).isEqualTo(assessment);
        }
        assertThat(CorrelationJsonCodec.writeEvidence(List.of(new Evidence.TemporalProximity(12, Duration.ofSeconds(-20)))))
                .contains("\"distanceToGroup\":\"PT-20S\"");
    }

    @Test
    void rejectsUnknownVersionsTypesAndInvalidShapes() {
        for (String json : List.of(
                "{\"formatVersion\":2,\"assessment\":{\"status\":\"UNDETERMINED\",\"reason\":\"INSUFFICIENT_EVIDENCE\"}}",
                "{\"formatVersion\":1,\"assessment\":{\"status\":\"OTHER\"}}",
                "{\"formatVersion\":1,\"assessment\":{\"status\":\"RANKED\",\"candidates\":[],\"evidence\":[]}}",
                "{\"formatVersion\":1,\"assessment\":{\"status\":\"UNDETERMINED\",\"reason\":\"free text\"}}")) {
            assertThatThrownBy(() -> CorrelationJsonCodec.readAssessment(json)).isInstanceOf(RuntimeException.class);
        }
        for (String json : List.of("{\"formatVersion\":1,\"items\":[]}",
                "{\"formatVersion\":1,\"items\":[{\"type\":\"OTHER\"}]}",
                "{\"formatVersion\":1,\"items\":[{\"type\":\"EARLIEST_ALERT\",\"alertId\":1}]}")) {
            assertThatThrownBy(() -> CorrelationJsonCodec.readEvidence(json)).isInstanceOf(RuntimeException.class);
        }
    }
}
