package ru.donskikh.incidenthub.alert;

public record AlertKey(String source, String externalId) {
    public AlertKey {
        if (source == null || source.isBlank() || source.length() > 100) {
            throw new IllegalArgumentException("source must contain 1 to 100 characters");
        }
        if (externalId == null || externalId.isBlank() || externalId.length() > 255) {
            throw new IllegalArgumentException("externalId must contain 1 to 255 characters");
        }
    }
}
