package ru.donskikh.incidenthub.alert.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.donskikh.incidenthub.alert.AlertProperties;
import ru.donskikh.incidenthub.alert.application.AcceptAlertsService;
import ru.donskikh.incidenthub.security.IntegrationAuthorities;

@RestController
@RequestMapping("/api/v1/alerts")
@Tag(name = "Alerts", description = "Monitoring signal ingestion")
@EnableConfigurationProperties(AlertProperties.class)
public class AlertController {
    private final AcceptAlertsService service;
    private final AlertWebMapper mapper;

    public AlertController(AcceptAlertsService service, AlertWebMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('" + IntegrationAuthorities.ALERT_INGEST + "')")
    @Operation(operationId = "acceptAlerts", summary = "Accept a batch of monitoring alerts",
            description = "Returns a result per item in input order. Duplicates ignore the incoming event body. "
                    + "Unknown services or occurredAt beyond receivedAt plus the configured maximum clock skew "
                    + "(default PT1M) reject only that item with UNKNOWN_SERVICE or OCCURRED_IN_FUTURE. "
                    + "Rejected items do not reserve their key; the first valid occurrence can be accepted. "
                    + "Invalid batch structure rejects the entire request.",
            security = @SecurityRequirement(name = "integrationToken"))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Batch processed"),
            @ApiResponse(responseCode = "400", description = "Invalid batch structure or maximum size exceeded",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    public AcceptAlertsResponse accept(@Valid @RequestBody AcceptAlertsRequest request) {
        return mapper.toResponse(service.accept(mapper.toCommand(request)));
    }
}
