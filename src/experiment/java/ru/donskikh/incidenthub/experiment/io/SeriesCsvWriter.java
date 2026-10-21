package ru.donskikh.incidenthub.experiment.io;

import ru.donskikh.incidenthub.experiment.series.SeriesResult;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class SeriesCsvWriter {
    private static final List<String> LOCALIZATION = List.of("localization_top1", "localization_top1_strict",
            "localization_a1", "localization_a1_strict", "localization_a3", "localization_a3_strict",
            "localization_a5", "localization_a5_strict", "ranked_coverage", "undetermined_failure_rate");
    public byte[] runs(SeriesResult result) {
        var columns = new ArrayList<>(result.runs().getFirst().metrics().numericValues().keySet());
        var header = new ArrayList<>(List.of("seed", "parameters")); header.addAll(columns);
        var csv = new StringBuilder(row(header));
        var codec = new SeriesJsonCodec();
        for (var run : result.runs()) {
            var values = new ArrayList<String>();
            values.add(Long.toString(run.seed())); values.add(codec.parameters(result.config(), run.seed()));
            for (String column : columns) values.add(run.metrics().counts().containsKey(column)
                    ? Long.toString(run.metrics().counts().get(column)) : number(run.metrics().rates().get(column)));
            csv.append(row(values));
        }
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }
    public byte[] summary(SeriesResult result) {
        var csv = new StringBuilder(row(List.of("metric", "defined_seed_count", "mean", "sample_standard_deviation")));
        var columns = new ArrayList<>(LOCALIZATION);
        result.summary().keySet().stream().filter(key -> !LOCALIZATION.contains(key)).forEach(columns::add);
        for (String column : columns) {
            var item = result.summary().get(column);
            csv.append(row(List.of(column, Long.toString(item.definedCount()), number(item.mean()), number(item.standardDeviation()))));
        }
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }
    private String number(Double value) { return value == null ? "" : Double.toString(value); }
    private String row(List<String> values) {
        return values.stream().map(value -> "\"" + value.replace("\"", "\"\"") + "\"").collect(java.util.stream.Collectors.joining(",")) + "\n";
    }
}
