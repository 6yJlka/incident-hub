package ru.donskikh.incidenthub.experiment.scenario;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;

public record GroundTruth(SortedMap<String, TruthLabel> byAlertKey, List<Failure> failures) {
    public GroundTruth {
        failures = Objects.requireNonNull(failures, "failures must not be null").stream()
                .sorted(Comparator.comparing(Failure::startedAt).thenComparing(Failure::id)).toList();
        var ids = new TreeSet<String>();
        for (Failure failure : failures) {
            if (!ids.add(failure.id())) throw new IllegalArgumentException("Duplicate failure id");
        }
        var copy = new TreeMap<>(Objects.requireNonNull(byAlertKey, "byAlertKey must not be null"));
        copy.forEach((key, label) -> {
            if (key == null || key.isBlank()) throw new IllegalArgumentException("Truth key must not be blank");
            Objects.requireNonNull(label, "truth label must not be null");
            if (label instanceof TruthLabel.FailureReference reference && !ids.contains(reference.failureId())) {
                throw new IllegalArgumentException("Truth references an unknown failure");
            }
        });
        byAlertKey = Collections.unmodifiableSortedMap(copy);
    }
}
