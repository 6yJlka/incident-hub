package ru.donskikh.incidenthub.experiment.cli;

import ru.donskikh.incidenthub.experiment.io.SeriesCsvWriter;
import ru.donskikh.incidenthub.experiment.io.SeriesJsonCodec;
import ru.donskikh.incidenthub.experiment.series.ExperimentSeriesRunner;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public final class ExperimentRunnerMain {
    private ExperimentRunnerMain() {}
    public static void main(String[] args) throws IOException {
        Objects.requireNonNull(args, "args must not be null");
        Path config = null;
        Path output = Path.of("build/experiments/run");
        for (int i = 0; i < args.length; i += 2) {
            if (i + 1 == args.length) throw new IllegalArgumentException("Expected --config FILE or --output DIRECTORY");
            switch (args[i]) {
                case "--config" -> config = Path.of(args[i + 1]);
                case "--output" -> output = Path.of(args[i + 1]);
                default -> throw new IllegalArgumentException("Unknown argument: " + args[i]);
            }
        }
        if (config == null) throw new IllegalArgumentException("--config is required");
        Path root = Path.of("build/experiments").toAbsolutePath().normalize();
        output = output.toAbsolutePath().normalize();
        if (!output.startsWith(root)) throw new IllegalArgumentException("Output must be inside build/experiments");
        Files.createDirectories(output);
        if (!output.toRealPath().startsWith(root.toRealPath())) throw new IllegalArgumentException("Output escapes through a symbolic link");
        for (String name : new String[]{"runs.csv", "summary.csv"}) {
            if (Files.isSymbolicLink(output.resolve(name))) throw new IllegalArgumentException("Output file must not be a symbolic link");
        }
        var result = new ExperimentSeriesRunner().run(new SeriesJsonCodec().read(Files.readAllBytes(config)));
        var writer = new SeriesCsvWriter();
        Files.write(output.resolve("runs.csv"), writer.runs(result));
        Files.write(output.resolve("summary.csv"), writer.summary(result));
        System.out.println("Experiment completed: " + result.runs().size() + " seeds; " + output);
    }
}
