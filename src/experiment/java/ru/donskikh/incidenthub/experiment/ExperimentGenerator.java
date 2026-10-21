package ru.donskikh.incidenthub.experiment;

import ru.donskikh.incidenthub.experiment.graph.DistortGraphCommand;
import ru.donskikh.incidenthub.experiment.graph.GraphDistorter;
import ru.donskikh.incidenthub.experiment.graph.GraphGenerator;
import ru.donskikh.incidenthub.experiment.io.DemoGraphLoader;
import ru.donskikh.incidenthub.experiment.scenario.ScenarioGenerationCommand;
import ru.donskikh.incidenthub.experiment.scenario.ScenarioGenerator;
import ru.donskikh.incidenthub.experiment.scenario.ScenarioSeeds;

import java.util.Objects;

public final class ExperimentGenerator {
    public GeneratedScenario generate(GenerationParameters command) {
        Objects.requireNonNull(command, "command must not be null");
        var trueGraph = new GraphGenerator(new DemoGraphLoader().load()).generate(command.graph());
        var observed = new GraphDistorter().distort(new DistortGraphCommand(trueGraph, command.distortion(), command.spuriousEdgeType()));
        var sample = new ScenarioGenerator().generate(new ScenarioGenerationCommand(trueGraph, command.scenario(), command.scenarioSeed()));
        return new GeneratedScenario(1, "1", command, ScenarioSeeds.from(command.scenarioSeed()), trueGraph,
                new ScenarioObservation(observed, sample.alerts()), sample.truth());
    }
}
