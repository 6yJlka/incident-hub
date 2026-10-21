package ru.donskikh.incidenthub.experiment.scenario;

import java.util.List;
import java.util.Objects;
import java.util.TreeSet;

public record ScenarioSample(List<ScenarioAlert> alerts, GroundTruth truth) {
    public ScenarioSample {
        alerts = Objects.requireNonNull(alerts, "alerts must not be null").stream().sorted(ScenarioAlert.ORDER).toList();
        Objects.requireNonNull(truth, "truth must not be null");
        var keys = new TreeSet<String>();
        for (ScenarioAlert alert : alerts) {
            if (!keys.add(alert.externalKey())) throw new IllegalArgumentException("Duplicate external alert key");
        }
        if (!keys.equals(truth.byAlertKey().keySet())) throw new IllegalArgumentException("Each alert needs exactly one truth label");
    }
}
