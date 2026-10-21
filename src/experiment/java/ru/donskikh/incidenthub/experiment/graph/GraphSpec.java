package ru.donskikh.incidenthub.experiment.graph;

import java.util.Objects;

public sealed interface GraphSpec {
    record Demo() implements GraphSpec {}

    record Tree(int depth, int branching) implements GraphSpec {
        public Tree {
            if (depth < 0 || branching < 1) throw new IllegalArgumentException("Invalid tree depth or branching");
        }
    }

    record Layered(int layers, int width, double density) implements GraphSpec {
        public Layered {
            if (layers < 1 || width < 1 || layers == 1 && width != 1) {
                throw new IllegalArgumentException("Layered graph needs at least two layers, or one singleton layer");
            }
            requireRate(density);
            long nodes = Math.multiplyExact((long) layers, width);
            long possible = Math.multiplyExact(Math.multiplyExact((long) layers - 1, width), width);
            if (Math.round(density * possible) < nodes - 1) {
                throw new IllegalArgumentException("Density is too small for a connected layered graph");
            }
        }
    }

    record Diamonds(int count, int branchDepth) implements GraphSpec {
        public Diamonds {
            if (count < 1 || branchDepth < 2) throw new IllegalArgumentException("Diamonds need count >= 1 and branchDepth >= 2");
        }
    }

    record Cyclic(Layered base, double reverseEdgeRate) implements GraphSpec {
        public Cyclic {
            Objects.requireNonNull(base, "base must not be null");
            requireRate(reverseEdgeRate);
            if (reverseEdgeRate == 0 || base.layers() < 2) {
                throw new IllegalArgumentException("Cyclic graph needs an edge and a positive reverse edge rate");
            }
        }
    }

    static void requireRate(double rate) {
        if (!Double.isFinite(rate) || rate < 0 || rate > 1) {
            throw new IllegalArgumentException("Rate must be finite and between zero and one");
        }
    }
}
