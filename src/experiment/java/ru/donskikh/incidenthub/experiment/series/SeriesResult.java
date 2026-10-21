package ru.donskikh.incidenthub.experiment.series;

import java.util.Collections;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
public record SeriesResult(ExperimentSeriesConfig config, List<ExperimentRun> runs, SortedMap<String, MetricSummary> summary) {
    public SeriesResult { runs = List.copyOf(runs); summary = Collections.unmodifiableSortedMap(new TreeMap<>(summary)); }
}
