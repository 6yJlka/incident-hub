package ru.donskikh.incidenthub.experiment.scenario;

import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public sealed interface FailurePlan {
    record Explicit(List<Failure> failures) implements FailurePlan {
        public Explicit {
            failures = Objects.requireNonNull(failures, "failures must not be null").stream()
                    .sorted(Comparator.comparing(Failure::startedAt).thenComparing(Failure::id)).toList();
            if (failures.stream().map(Failure::id).distinct().count() != failures.size()) {
                throw new IllegalArgumentException("Failure ids must be unique");
            }
        }
    }

    record Selected(int count, Duration firstStartOffset, Duration spacing, RootSelectionMode mode) implements FailurePlan {
        public Selected {
            if (count < 1) throw new IllegalArgumentException("Selected failure count must be positive");
            DelayDistribution.requireMillis(firstStartOffset);
            DelayDistribution.requireMillis(spacing);
            Objects.requireNonNull(mode, "mode must not be null");
        }
    }

    enum RootSelectionMode { UNIFORM_DISTINCT, PATH_CONNECTED_TO_FIRST }
}
