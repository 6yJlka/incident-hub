package ru.donskikh.incidenthub.alertgroup.application;

public record DismissAlertGroupCommand(long groupId, String reason, long actorId) {
    public DismissAlertGroupCommand {
        if (groupId <= 0 || actorId <= 0) {
            throw new IllegalArgumentException("groupId and actorId must be positive");
        }
        if (reason != null && reason.length() > 2000) {
            throw new IllegalArgumentException("reason must not exceed 2000 characters");
        }
    }
}
