package ru.donskikh.incidenthub.experiment.metrics;

import java.util.Collections;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

public record MetricsResult(SortedMap<String, Long> counts, SortedMap<String, Double> rates) {
    public MetricsResult {
        counts = Collections.unmodifiableSortedMap(new TreeMap<>(counts));
        rates = Collections.unmodifiableSortedMap(new TreeMap<>(rates));
        if (counts.values().stream().anyMatch(value -> value == null || value < 0)
                || rates.values().stream().anyMatch(value -> value != null && !Double.isFinite(value))) {
            throw new IllegalArgumentException("Invalid metric value");
        }
    }
    public SortedMap<String, Double> numericValues() {
        var values = new TreeMap<>(rates);
        counts.forEach((key, value) -> values.put(key, value.doubleValue()));
        return Collections.unmodifiableSortedMap(values);
    }
}
