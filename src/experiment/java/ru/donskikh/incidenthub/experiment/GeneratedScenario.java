package ru.donskikh.incidenthub.experiment;

import ru.donskikh.incidenthub.experiment.graph.ServiceGraph;
import ru.donskikh.incidenthub.experiment.scenario.GroundTruth;
import ru.donskikh.incidenthub.experiment.scenario.ScenarioSample;
import ru.donskikh.incidenthub.experiment.scenario.ScenarioSeeds;
import ru.donskikh.incidenthub.experiment.scenario.TruthLabel;

import java.util.Objects;
import java.util.TreeMap;
import java.util.TreeSet;

public record GeneratedScenario(int formatVersion, String modelVersion, GenerationParameters parameters,
        ScenarioSeeds randomSeeds, ServiceGraph trueGraph, ScenarioObservation observation, GroundTruth truth) {
    public GeneratedScenario {
        if (formatVersion != 1 || !"1".equals(modelVersion)) {
            throw new IllegalArgumentException("Unsupported experiment format or model version");
        }
        Objects.requireNonNull(parameters, "parameters must not be null");
        Objects.requireNonNull(trueGraph, "trueGraph must not be null");
        Objects.requireNonNull(observation, "observation must not be null");
        Objects.requireNonNull(truth, "truth must not be null");
        if (!ScenarioSeeds.from(parameters.scenarioSeed()).equals(randomSeeds)) {
            throw new IllegalArgumentException("Derived random seeds do not match the scenario seed");
        }
        if (!trueGraph.services().equals(observation.observedGraph().services())) {
            throw new IllegalArgumentException("Observed graph must preserve the true graph services");
        }
        new ScenarioSample(observation.alerts(), truth);
        var ids = new TreeSet<Long>();
        trueGraph.services().forEach(service -> ids.add(service.id()));
        var failures = new TreeMap<String, ru.donskikh.incidenthub.experiment.scenario.Failure>();
        truth.failures().forEach(failure -> {
            if (!ids.contains(failure.rootServiceId()) || failure.startedAt().isBefore(parameters.scenario().start())
                    || !failure.startedAt().isBefore(parameters.scenario().start().plus(parameters.scenario().injectionPeriod()))) {
                throw new IllegalArgumentException("Truth has an invalid root service or failure start");
            }
            failures.put(failure.id(), failure);
        });
        var seen = new TreeMap<String, TreeSet<Long>>();
        for (var alert : observation.alerts()) {
            if (alert.occurredAt().isBefore(parameters.scenario().start())) {
                throw new IllegalArgumentException("Alert precedes the injection period");
            }
            if (truth.byAlertKey().get(alert.externalKey()) instanceof TruthLabel.FailureReference label) {
                if (alert.occurredAt().isBefore(failures.get(label.failureId()).startedAt())
                        || !seen.computeIfAbsent(label.failureId(), ignored -> new TreeSet<>()).add(alert.serviceId())) {
                    throw new IllegalArgumentException("Failure alerts must follow the failure start and be unique per service");
                }
            } else if (!alert.occurredAt().isBefore(parameters.scenario().start().plus(parameters.scenario().injectionPeriod()))) {
                throw new IllegalArgumentException("Noise must lie within the injection period");
            }
        }
        truth.failures().forEach(failure -> {
            if (!seen.getOrDefault(failure.id(), new TreeSet<>()).contains(failure.rootServiceId())) {
                throw new IllegalArgumentException("Every failure must produce its root alert");
            }
        });
    }
}
