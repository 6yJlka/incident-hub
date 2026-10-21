package ru.donskikh.incidenthub.experiment.series;

import ru.donskikh.incidenthub.experiment.metrics.MetricsResult;
import java.util.Objects;
public record ExperimentRun(long seed, MetricsResult metrics) {
    public ExperimentRun { Objects.requireNonNull(metrics, "metrics must not be null"); }
}
