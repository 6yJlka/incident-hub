package ru.donskikh.incidenthub.experiment.scenario;

import ru.donskikh.incidenthub.alert.AlertSeverity;
import ru.donskikh.incidenthub.alert.AlertSignalType;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Random;

public record SignalDistribution(List<TypeWeight> types, List<SeverityWeight> severities) {
    public SignalDistribution {
        types = Objects.requireNonNull(types, "types must not be null").stream()
                .sorted(Comparator.comparing(weight -> weight.value().name())).toList();
        severities = Objects.requireNonNull(severities, "severities must not be null").stream()
                .sorted(Comparator.comparing(weight -> weight.value().name())).toList();
        if (types.isEmpty() || severities.isEmpty()
                || types.stream().map(TypeWeight::value).distinct().count() != types.size()
                || severities.stream().map(SeverityWeight::value).distinct().count() != severities.size()) {
            throw new IllegalArgumentException("Signal distributions must be nonempty and have unique values");
        }
        Math.toIntExact(types.stream().mapToLong(TypeWeight::weight).sum());
        Math.toIntExact(severities.stream().mapToLong(SeverityWeight::weight).sum());
    }

    public record TypeWeight(AlertSignalType value, int weight) {
        public TypeWeight { Objects.requireNonNull(value, "type must not be null"); requireWeight(weight); }
    }

    public record SeverityWeight(AlertSeverity value, int weight) {
        public SeverityWeight { Objects.requireNonNull(value, "severity must not be null"); requireWeight(weight); }
    }

    public record Signal(AlertSignalType type, AlertSeverity severity) {}

    public Signal sample(Random random) {
        int typeDraw = random.nextInt(types.stream().mapToInt(TypeWeight::weight).sum());
        int severityDraw = random.nextInt(severities.stream().mapToInt(SeverityWeight::weight).sum());
        AlertSignalType type = null;
        AlertSeverity severity = null;
        for (TypeWeight weight : types) {
            typeDraw -= weight.weight();
            if (typeDraw < 0) { type = weight.value(); break; }
        }
        for (SeverityWeight weight : severities) {
            severityDraw -= weight.weight();
            if (severityDraw < 0) { severity = weight.value(); break; }
        }
        return new Signal(type, severity);
    }

    private static void requireWeight(int weight) {
        if (weight <= 0) throw new IllegalArgumentException("Signal weight must be positive; omit zero-weight values");
    }
}
