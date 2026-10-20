package ru.donskikh.incidenthub.experiment;

import ru.donskikh.incidenthub.experiment.graph.ServiceGraph;
import ru.donskikh.incidenthub.experiment.scenario.ScenarioAlert;

import java.util.List;
import java.util.Objects;
import java.util.TreeSet;

public record ScenarioObservation(ServiceGraph observedGraph, List<ScenarioAlert> alerts) {
    public ScenarioObservation {
        Objects.requireNonNull(observedGraph, "observedGraph must not be null");
        alerts = Objects.requireNonNull(alerts, "alerts must not be null").stream().sorted(ScenarioAlert.ORDER).toList();
        var serviceIds = new TreeSet<Long>();
        observedGraph.services().forEach(service -> serviceIds.add(service.id()));
        var keys = new TreeSet<String>();
        for (ScenarioAlert alert : alerts) {
            if (!serviceIds.contains(alert.serviceId()) || !keys.add(alert.externalKey())) {
                throw new IllegalArgumentException("Observation has an unknown service or duplicate alert key");
            }
        }
    }
}
