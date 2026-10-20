package ru.donskikh.incidenthub.experiment.scenario;

import java.util.Objects;

public record SignalModel(SignalDistribution root, SignalDistribution propagated, SignalDistribution noise) {
    public SignalModel {
        Objects.requireNonNull(root, "root signals must not be null");
        Objects.requireNonNull(propagated, "propagated signals must not be null");
        Objects.requireNonNull(noise, "noise signals must not be null");
    }
}
