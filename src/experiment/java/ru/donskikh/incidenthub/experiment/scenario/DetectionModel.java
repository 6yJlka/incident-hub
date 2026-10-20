package ru.donskikh.incidenthub.experiment.scenario;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.SortedMap;
import java.util.TreeMap;

public record DetectionModel(DelayDistribution defaultDistribution, SortedMap<Long, DelayDistribution> overrides) {
    public DetectionModel {
        Objects.requireNonNull(defaultDistribution, "defaultDistribution must not be null");
        var copy = new TreeMap<>(Objects.requireNonNull(overrides, "overrides must not be null"));
        copy.forEach((id, distribution) -> {
            if (id <= 0) throw new IllegalArgumentException("Detection service id must be positive");
            Objects.requireNonNull(distribution, "detection override must not be null");
        });
        overrides = Collections.unmodifiableSortedMap(copy);
    }

    public DetectionModel(DelayDistribution defaultDistribution, Map<Long, DelayDistribution> overrides) {
        this(defaultDistribution, new TreeMap<>(overrides));
    }
}
