package ru.donskikh.incidenthub.alert.web;

import java.util.List;

public record AcceptAlertsResponse(List<AlertAcceptanceResponse> results) {
    public AcceptAlertsResponse {
        results = List.copyOf(results);
    }
}
