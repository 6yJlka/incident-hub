package ru.donskikh.incidenthub.experiment.series;

public record MetricSummary(long definedCount, Double mean, Double standardDeviation) {
    public MetricSummary {
        if (definedCount < 0 || (definedCount == 0) != (mean == null) || (definedCount < 2) != (standardDeviation == null)
                || mean != null && !Double.isFinite(mean) || standardDeviation != null && (!Double.isFinite(standardDeviation) || standardDeviation < 0)) {
            throw new IllegalArgumentException("Invalid metric summary");
        }
    }
}
