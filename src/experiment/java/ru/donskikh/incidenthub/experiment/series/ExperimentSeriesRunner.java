package ru.donskikh.incidenthub.experiment.series;

import ru.donskikh.incidenthub.experiment.ExperimentGenerator;
import ru.donskikh.incidenthub.experiment.metrics.ExperimentMetrics;
import ru.donskikh.incidenthub.experiment.replay.ScenarioReplay;
import java.util.ArrayList;
import java.util.Objects;
import java.util.TreeMap;

public final class ExperimentSeriesRunner {
    public SeriesResult run(ExperimentSeriesConfig command) {
        Objects.requireNonNull(command, "command must not be null");
        var runs = new ArrayList<ExperimentRun>();
        for (long seed : command.scenarioSeeds()) {
            var scenario = new ExperimentGenerator().generate(command.generation(seed));
            var replay = new ScenarioReplay().replay(scenario.observation(), command.correlator().create());
            runs.add(new ExperimentRun(seed, new ExperimentMetrics().calculate(replay, scenario.truth())));
        }
        var summary = new TreeMap<String, MetricSummary>();
        for (String metric : runs.getFirst().metrics().numericValues().keySet()) {
            long count = 0;
            double mean = 0, m2 = 0;
            for (var run : runs) {
                Double value = run.metrics().numericValues().get(metric);
                if (value == null) continue;
                count++;
                double delta = value - mean;
                mean += delta / count;
                m2 += delta * (value - mean);
            }
            summary.put(metric, new MetricSummary(count, count == 0 ? null : mean, count < 2 ? null : StrictMath.sqrt(Math.max(0, m2 / (count - 1)))));
        }
        return new SeriesResult(command, runs, summary);
    }
}
