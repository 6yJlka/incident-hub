package ru.donskikh.incidenthub.experiment.scenario;

import java.time.Duration;
import java.util.Objects;
import java.util.Random;

public sealed interface DelayDistribution {
    long sampleMillis(Random random);

    record Fixed(Duration value) implements DelayDistribution {
        public Fixed { requireMillis(value); }
        @Override public long sampleMillis(Random random) { return value.toMillis(); }
    }

    record Uniform(Duration minimum, Duration maximum) implements DelayDistribution {
        public Uniform {
            requireMillis(minimum);
            requireMillis(maximum);
            if (minimum.compareTo(maximum) > 0 || maximum.toMillis() == Long.MAX_VALUE) {
                throw new IllegalArgumentException("Invalid uniform delay bounds");
            }
        }
        @Override public long sampleMillis(Random random) {
            return random.nextLong(minimum.toMillis(), maximum.toMillis() + 1);
        }
    }

    static void requireMillis(Duration duration) {
        Objects.requireNonNull(duration, "duration must not be null");
        if (duration.isNegative() || !duration.equals(Duration.ofMillis(duration.toMillis()))) {
            throw new IllegalArgumentException("Duration must be nonnegative and aligned to milliseconds");
        }
    }
}
