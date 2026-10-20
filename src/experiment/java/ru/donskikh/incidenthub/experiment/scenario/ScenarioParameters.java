package ru.donskikh.incidenthub.experiment.scenario;

import ru.donskikh.incidenthub.experiment.graph.GraphSpec;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

public record ScenarioParameters(Instant start, Duration injectionPeriod, FailurePlan failures,
        double propagationProbability, DelayDistribution propagationDelay, DetectionModel detection,
        NoiseModel noise, SignalModel signals) {
    public ScenarioParameters {
        Objects.requireNonNull(start, "start must not be null");
        if (start.getNano() % 1_000_000 != 0) throw new IllegalArgumentException("Start must be aligned to milliseconds");
        DelayDistribution.requireMillis(injectionPeriod);
        if (injectionPeriod.isZero()) throw new IllegalArgumentException("Injection period must be positive");
        start.plus(injectionPeriod);
        Objects.requireNonNull(failures, "failures must not be null");
        GraphSpec.requireRate(propagationProbability);
        Objects.requireNonNull(propagationDelay, "propagationDelay must not be null");
        Objects.requireNonNull(detection, "detection must not be null");
        Objects.requireNonNull(noise, "noise must not be null");
        Objects.requireNonNull(signals, "signals must not be null");
    }
}
