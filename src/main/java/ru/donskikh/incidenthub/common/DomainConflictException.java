package ru.donskikh.incidenthub.common;

public abstract class DomainConflictException extends RuntimeException {

    private final Object currentState;
    private final Object requiredState;

    protected DomainConflictException(String message) {
        this(message, null, null, null);
    }

    protected DomainConflictException(String message, Throwable cause) {
        this(message, cause, null, null);
    }

    protected DomainConflictException(String message, Object currentState, Object requiredState) {
        this(message, null, currentState, requiredState);
    }

    private DomainConflictException(
            String message,
            Throwable cause,
            Object currentState,
            Object requiredState
    ) {
        super(message, cause);
        this.currentState = currentState;
        this.requiredState = requiredState;
    }

    public Object getCurrentState() {
        return currentState;
    }

    public Object getRequiredState() {
        return requiredState;
    }
}
