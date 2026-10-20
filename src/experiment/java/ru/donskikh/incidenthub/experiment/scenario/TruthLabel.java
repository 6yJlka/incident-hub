package ru.donskikh.incidenthub.experiment.scenario;

public sealed interface TruthLabel {
    record FailureReference(String failureId) implements TruthLabel {
        public FailureReference {
            if (failureId == null || failureId.isBlank()) throw new IllegalArgumentException("failureId must not be blank");
        }
    }
    record Noise() implements TruthLabel {}
}
