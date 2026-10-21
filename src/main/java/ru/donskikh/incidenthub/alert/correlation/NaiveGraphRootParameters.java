package ru.donskikh.incidenthub.alert.correlation;

import ru.donskikh.incidenthub.alert.AlertSeverity;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

public record NaiveGraphRootParameters(Map<AlertSeverity, Double> severityWeights, double damping,
        double tolerance, int maxIterations) {
    public NaiveGraphRootParameters {
        var copy = new TreeMap<>(Objects.requireNonNull(severityWeights, "severityWeights must not be null"));
        if (copy.size() != AlertSeverity.values().length) throw new IllegalArgumentException("Every severity needs a weight");
        copy.values().forEach(weight -> {
            if (weight == null || !Double.isFinite(weight) || weight <= 0) throw new IllegalArgumentException("Weights must be finite and positive");
        });
        severityWeights = Collections.unmodifiableMap(copy);
        if (!Double.isFinite(damping) || damping < 0 || damping >= 1 || !Double.isFinite(tolerance)
                || tolerance <= 0 || maxIterations <= 0) throw new IllegalArgumentException("Invalid PageRank convergence parameters");
    }
    public static NaiveGraphRootParameters defaults() {
        return new NaiveGraphRootParameters(Map.of(AlertSeverity.CRITICAL, 3.0, AlertSeverity.WARNING, 2.0, AlertSeverity.INFO, 1.0), .85, 1e-12, 1000);
    }
}
