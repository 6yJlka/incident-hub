package ru.donskikh.incidenthub.identity;

public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(long userId) {
        super("User not found: " + userId);
    }
}
