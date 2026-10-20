package ru.donskikh.incidenthub.experiment.cli;

import ru.donskikh.incidenthub.experiment.ExperimentDefaults;
import ru.donskikh.incidenthub.experiment.ExperimentGenerator;
import ru.donskikh.incidenthub.experiment.io.ScenarioJsonCodec;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public final class ExperimentGeneratorMain {
    private ExperimentGeneratorMain() {}

    public static void main(String[] args) throws IOException {
        Objects.requireNonNull(args, "args must not be null");
        Path configuration = null;
        Path output = Path.of("build/experiments/scenario.json");
        for (int index = 0; index < args.length; index += 2) {
            if (index + 1 == args.length) throw new IllegalArgumentException("Expected --config FILE or --output FILE");
            switch (args[index]) {
                case "--config" -> configuration = Path.of(args[index + 1]);
                case "--output" -> output = Path.of(args[index + 1]);
                default -> throw new IllegalArgumentException("Unknown argument: " + args[index]);
            }
        }
        Path root = Path.of("build/experiments").toAbsolutePath().normalize();
        output = output.toAbsolutePath().normalize();
        if (!output.startsWith(root) || output.equals(root)) {
            throw new IllegalArgumentException("Experiment output must be a file inside build/experiments");
        }
        var codec = new ScenarioJsonCodec();
        var parameters = configuration == null ? ExperimentDefaults.parameters() : codec.readParameters(Files.readAllBytes(configuration));
        var scenario = new ExperimentGenerator().generate(parameters);
        Files.createDirectories(output.getParent());
        if (!output.getParent().toRealPath().startsWith(root.toRealPath()) || Files.isSymbolicLink(output)) {
            throw new IllegalArgumentException("Experiment output must not escape build/experiments through a symbolic link");
        }
        Files.write(output, codec.write(scenario));
        System.out.println("Scenario written to " + output + "; alerts=" + scenario.observation().alerts().size());
    }
}
