package ru.donskikh.incidenthub.alertgroup.web;

import ru.donskikh.incidenthub.alert.correlation.RootAssessment;
import ru.donskikh.incidenthub.alert.correlation.RootAssessmentStatus;
import ru.donskikh.incidenthub.alertgroup.application.RootCandidateService;

import java.util.List;
import java.util.Map;

public record RootAssessmentResponse(RootAssessmentStatus status, List<RootCandidateResponse> candidates,
        List<EvidenceResponse> evidence, String reason) {
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
