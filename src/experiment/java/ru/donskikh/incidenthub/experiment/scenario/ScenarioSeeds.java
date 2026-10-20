package ru.donskikh.incidenthub.experiment.scenario;

public record ScenarioSeeds(long failures, long noise, long signals) {
    public static ScenarioSeeds from(long seed) {
        return new ScenarioSeeds(mix(seed ^ 0x4641494c55524553L), mix(seed ^ 0x4e4f495345000001L),
                mix(seed ^ 0x5349474e414c5301L));
    }

    private static long mix(long value) {
        value = (value ^ (value >>> 30)) * 0xbf58476d1ce4e5b9L;
        value = (value ^ (value >>> 27)) * 0x94d049bb133111ebL;
        return value ^ (value >>> 31);
    }
}
