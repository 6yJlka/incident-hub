package ru.donskikh.incidenthub.alert.correlation;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;

public sealed interface RootAssessment {
    record Ranked(List<RootCandidate> candidates, List<Evidence> evidence) implements RootAssessment {
        public Ranked {
            candidates = List.copyOf(Objects.requireNonNull(candidates, "candidates must not be null"));
            evidence = Evidence.requireEvidence(evidence);
            if (candidates.isEmpty()) throw new IllegalArgumentException("Ranked candidates must not be empty");
            var services = new HashSet<Long>();
            double previous = Double.POSITIVE_INFINITY;
            for (RootCandidate candidate : candidates) {
                if (!services.add(candidate.serviceId()) || candidate.score() > previous) {
                    throw new IllegalArgumentException("Candidates must have unique services and descending scores");
                }
                previous = candidate.score();
            }
        }
    }

    record Undetermined(String reason) implements RootAssessment {
        public Undetermined {
            if (reason == null || !reason.matches("[A-Z][A-Z0-9_]*")) {
                throw new IllegalArgumentException("Undetermined reason must be a stable uppercase code");
            }
        }
    }
}
