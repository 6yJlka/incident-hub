package ru.donskikh.incidenthub.alert.correlation;



public enum RootAssessmentStatus {
    RANKED, UNDETERMINED;

    public static RootAssessmentStatus of(RootAssessment assessment) {
        return switch (assessment) {
            case RootAssessment.Ranked ignored -> RANKED;
            case RootAssessment.Undetermined ignored -> UNDETERMINED;
        };
    }
}
