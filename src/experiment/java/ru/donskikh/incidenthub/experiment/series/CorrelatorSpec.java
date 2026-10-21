package ru.donskikh.incidenthub.experiment.series;

import ru.donskikh.incidenthub.alert.correlation.*;
import java.time.Duration;
import java.util.Objects;

public sealed interface CorrelatorSpec {
    AlertCorrelator create();
    record TimeWindow(Duration window) implements CorrelatorSpec {
        public TimeWindow { new TimeWindowCorrelator(window); }
        public AlertCorrelator create() { return new TimeWindowCorrelator(window); }
    }
    record NaiveGraphRoot(Duration window, NaiveGraphRootParameters root) implements CorrelatorSpec {
        public NaiveGraphRoot { new TimeWindowCorrelator(window); Objects.requireNonNull(root, "root must not be null"); }
        public AlertCorrelator create() { return new NaiveGraphRootCorrelator(window, root); }
    }
}
