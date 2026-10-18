package ru.donskikh.incidenthub.alert.correlation;

public interface AlertCorrelator {

    CorrelationDecision correlate(CorrelationAlert alert, CorrelationContext context);
}
