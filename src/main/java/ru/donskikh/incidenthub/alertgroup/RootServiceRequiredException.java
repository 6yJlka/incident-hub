package ru.donskikh.incidenthub.alertgroup;



public class RootServiceRequiredException extends RuntimeException {
    public RootServiceRequiredException() {
        super("businessServiceId is required when the probable root is undetermined");
    }
}
