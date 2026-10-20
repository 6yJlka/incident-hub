package ru.donskikh.incidenthub.alertgroup.web;

import io.swagger.v3.oas.annotations.media.Schema;
import ru.donskikh.incidenthub.alert.correlation.RootAssessment;
import ru.donskikh.incidenthub.alert.correlation.RootAssessmentStatus;
import ru.donskikh.incidenthub.alertgroup.application.RootCandidateService;

import java.util.List;
import java.util.Map;

@Schema(additionalProperties = Schema.AdditionalPropertiesValue.FALSE,
        description = "Root assessment. RANKED has nonempty candidates and evidence and a null reason. "
                + "UNDETERMINED has empty candidates and evidence and a stable reason code.")
public record RootAssessmentResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) RootAssessmentStatus status,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED,
                description = "Ranked candidates in descending score order for RANKED; empty for UNDETERMINED")
        List<RootCandidateResponse> candidates,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED,
                description = "Nonempty root assessment evidence for RANKED; empty for UNDETERMINED")
        List<EvidenceResponse> evidence,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, types = {"string", "null"},
                description = "Null for RANKED; stable reason code such as INSUFFICIENT_EVIDENCE for UNDETERMINED")
        String reason) {
    public RootAssessmentResponse {
        candidates = List.copyOf(candidates);
        evidence = List.copyOf(evidence);
    }

    static RootAssessmentResponse from(RootAssessment assessment, Map<Long, RootCandidateService> services) {
        return switch (assessment) {
            case RootAssessment.Ranked ranked -> new RootAssessmentResponse(RootAssessmentStatus.RANKED,
                    ranked.candidates().stream().map(candidate -> {
                        var service = services.get(candidate.serviceId());
                        return new RootCandidateResponse(candidate.serviceId(), candidate.score(),
                                service == null ? null : service.code(), service == null ? null : service.name());
                    }).toList(), ranked.evidence().stream().map(EvidenceResponse::from).toList(), null);
            case RootAssessment.Undetermined undetermined -> new RootAssessmentResponse(RootAssessmentStatus.UNDETERMINED,
                    List.of(), List.of(), undetermined.reason());
        };
    }
}
