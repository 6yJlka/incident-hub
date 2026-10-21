package ru.donskikh.incidenthub.experiment.scenario;

import ru.donskikh.incidenthub.experiment.graph.GraphSpec;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.SortedMap;
import java.util.TreeMap;

public record DetectionModel(DelayDistribution defaultDistribution, SortedMap<Long, DelayDistribution> overrides,
        double detectionProbability, SortedMap<Long, Double> probabilityOverrides) {
    public DetectionModel {
        Objects.requireNonNull(defaultDistribution, "defaultDistribution must not be null");
        var copy = new TreeMap<>(Objects.requireNonNull(overrides, "overrides must not be null"));
        copy.forEach((id, distribution) -> {
            if (id <= 0) throw new IllegalArgumentException("Detection service id must be positive");
            Objects.requireNonNull(distribution, "detection override must not be null");
        });
        overrides = Collections.unmodifiableSortedMap(copy);
        GraphSpec.requireRate(detectionProbability);
        var probabilities = new TreeMap<>(Objects.requireNonNull(probabilityOverrides, "probabilityOverrides must not be null"));
        probabilities.forEach((id, probability) -> {
            if (id <= 0) throw new IllegalArgumentException("Detection service id must be positive");
            GraphSpec.requireRate(Objects.requireNonNull(probability, "detection probability override must not be null"));
        });
        probabilityOverrides = Collections.unmodifiableSortedMap(probabilities);
    }

    public DetectionModel(DelayDistribution defaultDistribution, Map<Long, DelayDistribution> overrides) {
        this(defaultDistribution, new TreeMap<>(overrides), 1.0, new TreeMap<>());
    }

    public DetectionModel(DelayDistribution defaultDistribution, Map<Long, DelayDistribution> overrides,
            double detectionProbability, Map<Long, Double> probabilityOverrides) {
        this(defaultDistribution, new TreeMap<>(overrides), detectionProbability, new TreeMap<>(probabilityOverrides));
    }
}
