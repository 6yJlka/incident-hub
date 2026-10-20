package ru.donskikh.incidenthub.experiment.io;

import ru.donskikh.incidenthub.experiment.graph.ServiceGraph;

import java.io.IOException;

public final class DemoGraphLoader {
    public ServiceGraph load() {
        try (var input = DemoGraphLoader.class.getResourceAsStream("/experiment/graphs/demo-v1.json")) {
            if (input == null) throw new IllegalStateException("Experiment demo graph resource is missing");
            return new ScenarioJsonCodec().readGraph(input.readAllBytes());
        } catch (IOException exception) {
            throw new IllegalStateException("Could not read the experiment demo graph", exception);
        }
    }
}
