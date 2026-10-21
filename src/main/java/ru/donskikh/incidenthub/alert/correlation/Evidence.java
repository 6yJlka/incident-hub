package ru.donskikh.incidenthub.alert.correlation;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

public sealed interface Evidence {
    record TemporalProximity(long alertId, Duration distanceToGroup) implements Evidence {
        public TemporalProximity {
            if (alertId <= 0) throw new IllegalArgumentException("alertId must be positive");
            Objects.requireNonNull(distanceToGroup, "distanceToGroup must not be null");
        }
    }

    record EarliestAlert(long alertId, long serviceId) implements Evidence {
        public EarliestAlert {
            if (alertId <= 0 || serviceId <= 0) {
                throw new IllegalArgumentException("Alert and service identifiers must be positive");
            }
        }
    }

    record DependencyPath(List<Long> serviceIds) implements Evidence {
        public DependencyPath {
            serviceIds = List.copyOf(Objects.requireNonNull(serviceIds, "serviceIds must not be null"));
            if (serviceIds.size() < 2 || serviceIds.stream().anyMatch(id -> id <= 0)) {
                throw new IllegalArgumentException("Dependency path must contain at least two positive service ids");
            }
        }
    }

    record PageRankScore(long serviceId, double score, double anomalyWeight, double damping,
                         int iterations, double residual) implements Evidence {
        public PageRankScore {
            if (serviceId <= 0 || !Double.isFinite(score) || score < 0 || !Double.isFinite(anomalyWeight) || anomalyWeight <= 0
                    || !Double.isFinite(damping) || damping < 0 || damping >= 1 || iterations <= 0
                    || !Double.isFinite(residual) || residual < 0) throw new IllegalArgumentException("Invalid PageRank evidence");
        }
    }

    record Migrated() implements Evidence {}

    static List<Evidence> requireEvidence(List<Evidence> evidence) {
        var copy = List.copyOf(Objects.requireNonNull(evidence, "evidence must not be null"));
        if (copy.isEmpty()) throw new IllegalArgumentException("Evidence must not be empty");
        return copy;
    }
}
