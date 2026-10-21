package ru.donskikh.incidenthub.experiment.scenario;

public record NoiseModel(double ratePerMinute) {
    public NoiseModel {
        if (!Double.isFinite(ratePerMinute) || ratePerMinute < 0) {
            throw new IllegalArgumentException("Noise intensity must be finite and nonnegative");
        }
    }
}
