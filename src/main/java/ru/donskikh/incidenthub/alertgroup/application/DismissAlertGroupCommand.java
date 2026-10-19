package ru.donskikh.incidenthub.alertgroup.application;

public record DismissAlertGroupCommand(long groupId, String reason, long actorId, long expectedVersion) {
    public DismissAlertGroupCommand {
        if (expectedVersion < 0) {
            throw new IllegalArgumentException("expectedVersion must not be negative");
        }
        if (groupId <= 0 || actorId <= 0) {
            throw new IllegalArgumentException("groupId and actorId must be positive");
        }
        if (reason != null && reason.length() > 2000) {
            throw new IllegalArgumentException("reason must not exceed 2000 characters");
        }
    }
}
