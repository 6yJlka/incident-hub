package ru.donskikh.incidenthub.alert.correlation;



public record RootCandidate(long serviceId, double score) {
    public RootCandidate {
        if (serviceId <= 0 || !Double.isFinite(score)) {
            throw new IllegalArgumentException("Service identifier must be positive and score must be finite");
        }
    }
}
