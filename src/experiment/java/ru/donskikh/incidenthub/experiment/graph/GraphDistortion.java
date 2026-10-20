package ru.donskikh.incidenthub.experiment.graph;

public record GraphDistortion(double missingEdgeRate, double spuriousEdgeRate, long seed) {
    public GraphDistortion {
        GraphSpec.requireRate(missingEdgeRate);
        GraphSpec.requireRate(spuriousEdgeRate);
    }
}
