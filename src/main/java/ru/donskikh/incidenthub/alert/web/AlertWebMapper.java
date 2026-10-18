package ru.donskikh.incidenthub.alert.web;

import org.springframework.stereotype.Component;
import ru.donskikh.incidenthub.alert.application.AcceptAlertsCommand;
import ru.donskikh.incidenthub.alert.application.AcceptAlertsResult;
import ru.donskikh.incidenthub.alert.application.IncomingAlert;

@Component
public class AlertWebMapper {
    public AcceptAlertsCommand toCommand(AcceptAlertsRequest request) {
        return new AcceptAlertsCommand(request.alerts().stream().map(alert -> new IncomingAlert(
                alert.source(), alert.externalId(), alert.serviceCode(), alert.signalType(),
                alert.severity(), alert.occurredAt(), alert.description()
        )).toList());
    }

    public AcceptAlertsResponse toResponse(AcceptAlertsResult result) {
        return new AcceptAlertsResponse(result.results().stream().map(item -> new AlertAcceptanceResponse(
                item.externalId(), item.status(), item.alertId(), item.reason()
        )).toList());
    }
}
