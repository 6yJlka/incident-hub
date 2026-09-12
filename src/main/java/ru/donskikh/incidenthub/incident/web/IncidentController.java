package ru.donskikh.incidenthub.incident.web;

import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import ru.donskikh.incidenthub.audit.application.GetIncidentHistoryService;
import ru.donskikh.incidenthub.incident.IncidentPriority;
import ru.donskikh.incidenthub.incident.IncidentSeverity;
import ru.donskikh.incidenthub.incident.IncidentSource;
import ru.donskikh.incidenthub.incident.IncidentStatus;
import ru.donskikh.incidenthub.incident.application.AssignIncidentService;
import ru.donskikh.incidenthub.incident.application.CancelIncidentCommand;
import ru.donskikh.incidenthub.incident.application.CancelIncidentService;
import ru.donskikh.incidenthub.incident.application.CloseIncidentCommand;
import ru.donskikh.incidenthub.incident.application.CloseIncidentService;
import ru.donskikh.incidenthub.incident.application.CreateIncidentService;
import ru.donskikh.incidenthub.incident.application.GetIncidentService;
import ru.donskikh.incidenthub.incident.application.ListIncidentsService;
import ru.donskikh.incidenthub.incident.application.ReopenIncidentCommand;
import ru.donskikh.incidenthub.incident.application.ReopenIncidentService;
import ru.donskikh.incidenthub.incident.application.ResolveIncidentCommand;
import ru.donskikh.incidenthub.incident.application.ResolveIncidentService;
import ru.donskikh.incidenthub.incident.application.StartIncidentProgressCommand;
import ru.donskikh.incidenthub.incident.application.StartIncidentProgressService;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/incidents")
@Tag(name = "Incidents", description = "Register incidents, inspect history, and perform explicit lifecycle transitions")
public class IncidentController {

    private final CreateIncidentService createIncidentService;
    private final ListIncidentsService listIncidentsService;
    private final GetIncidentService getIncidentService;
    private final GetIncidentHistoryService getIncidentHistoryService;
    private final AssignIncidentService assignIncidentService;
    private final StartIncidentProgressService startIncidentProgressService;
    private final ResolveIncidentService resolveIncidentService;
    private final CloseIncidentService closeIncidentService;
    private final ReopenIncidentService reopenIncidentService;
    private final CancelIncidentService cancelIncidentService;
    private final IncidentWebMapper mapper;

    public IncidentController(
            CreateIncidentService createIncidentService,
            ListIncidentsService listIncidentsService,
            GetIncidentService getIncidentService,
            GetIncidentHistoryService getIncidentHistoryService,
            AssignIncidentService assignIncidentService,
            StartIncidentProgressService startIncidentProgressService,
            ResolveIncidentService resolveIncidentService,
            CloseIncidentService closeIncidentService,
            ReopenIncidentService reopenIncidentService,
            CancelIncidentService cancelIncidentService,
            IncidentWebMapper mapper
    ) {
        this.createIncidentService = createIncidentService;
        this.listIncidentsService = listIncidentsService;
        this.getIncidentService = getIncidentService;
        this.getIncidentHistoryService = getIncidentHistoryService;
        this.assignIncidentService = assignIncidentService;
        this.startIncidentProgressService = startIncidentProgressService;
        this.resolveIncidentService = resolveIncidentService;
        this.closeIncidentService = closeIncidentService;
        this.reopenIncidentService = reopenIncidentService;
        this.cancelIncidentService = cancelIncidentService;
        this.mapper = mapper;
    }

    @PostMapping
    @Operation(
            summary = "Create an incident",
            description = "Registers a manual incident in OPEN status for an existing service and reporter. The service owner is used as the responsible team when no team is supplied."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Incident created in OPEN status"),
            @ApiResponse(responseCode = "400", description = "Request validation failed",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Reporter, affected service, or responsible team was not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<CreateIncidentResponse> create(
            @Valid @RequestBody CreateIncidentRequest request
    ) {
        CreateIncidentResponse response = mapper.toResponse(
                createIncidentService.create(mapper.toCommand(request))
        );
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.incidentId())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping
    @Operation(
            summary = "List incidents",
            description = "Returns incidents ordered by creation time with optional lifecycle, urgency, impact, source, service, and team filters."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Page of incidents returned"),
            @ApiResponse(responseCode = "400", description = "Pagination or filter parameter is invalid",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ListIncidentsResponse list(
            @Parameter(description = "Zero-based page index", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size from 1 to 100", example = "20")
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Filter by current lifecycle status", example = "IN_PROGRESS")
            @RequestParam(required = false) IncidentStatus status,
            @Parameter(description = "Filter by work ordering urgency", example = "HIGH")
            @RequestParam(required = false) IncidentPriority priority,
            @Parameter(description = "Filter by business impact level, independent of priority", example = "SEV2")
            @RequestParam(required = false) IncidentSeverity severity,
            @Parameter(description = "Filter by registration source", example = "MANUAL")
            @RequestParam(required = false) IncidentSource source,
            @Parameter(description = "Filter by affected service identifier", example = "42")
            @RequestParam(required = false) Long affectedServiceId,
            @Parameter(description = "Filter by responsible team identifier", example = "12")
            @RequestParam(required = false) Long responsibleTeamId
    ) {
        return mapper.toResponse(listIncidentsService.execute(mapper.toQuery(
                page,
                size,
                status,
                priority,
                severity,
                source,
                affectedServiceId,
                responsibleTeamId
        )));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get an incident", description = "Returns the current incident details and assignments.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Incident returned"),
            @ApiResponse(responseCode = "400", description = "Incident identifier is invalid",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Incident was not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public IncidentResponse get(
            @Parameter(description = "Incident identifier", example = "73") @PathVariable long id
    ) {
        return mapper.toResponse(getIncidentService.get(id));
    }

    @GetMapping("/{id}/history")
    @Operation(
            summary = "Get incident history",
            description = "Returns lifecycle audit events in chronological order."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Incident history returned"),
            @ApiResponse(responseCode = "400", description = "Incident identifier is invalid",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Incident was not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public IncidentHistoryResponse history(
            @Parameter(description = "Incident identifier", example = "73") @PathVariable long id
    ) {
        return mapper.toResponse(getIncidentHistoryService.get(id));
    }

    @PostMapping("/{id}/assign")
    @Operation(
            summary = "Assign an incident",
            description = "Assigns or reassigns an existing user. Allowed only while the incident is OPEN or ASSIGNED."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Incident assigned and current details returned"),
            @ApiResponse(responseCode = "400", description = "Incident or assignee identifier is invalid",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Incident or assignee was not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Current incident status does not allow assignment",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public IncidentResponse assign(
            @Parameter(description = "Incident identifier", example = "73") @PathVariable long id,
            @Valid @RequestBody AssignIncidentRequest request
    ) {
        long incidentId = assignIncidentService.assign(mapper.toCommand(id, request)).incidentId();
        return currentIncident(incidentId);
    }

    @PostMapping("/{id}/start")
    @Operation(
            summary = "Start incident work",
            description = "Moves an ASSIGNED incident to IN_PROGRESS. No other source status is allowed."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Incident moved to IN_PROGRESS"),
            @ApiResponse(responseCode = "400", description = "Incident identifier is invalid",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Incident was not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Incident is not ASSIGNED",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public IncidentResponse start(
            @Parameter(description = "Incident identifier", example = "73") @PathVariable long id
    ) {
        long incidentId = startIncidentProgressService.start(new StartIncidentProgressCommand(id)).incidentId();
        return currentIncident(incidentId);
    }

    @PostMapping("/{id}/resolve")
    @Operation(
            summary = "Resolve an incident",
            description = "Moves an IN_PROGRESS incident to RESOLVED. No other source status is allowed."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Incident moved to RESOLVED"),
            @ApiResponse(responseCode = "400", description = "Incident identifier is invalid",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Incident was not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Incident is not IN_PROGRESS",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public IncidentResponse resolve(
            @Parameter(description = "Incident identifier", example = "73") @PathVariable long id
    ) {
        long incidentId = resolveIncidentService.resolve(new ResolveIncidentCommand(id)).incidentId();
        return currentIncident(incidentId);
    }

    @PostMapping("/{id}/close")
    @Operation(
            summary = "Close an incident",
            description = "Moves a RESOLVED incident to CLOSED. No other source status is allowed."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Incident moved to CLOSED"),
            @ApiResponse(responseCode = "400", description = "Incident identifier is invalid",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Incident was not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Incident is not RESOLVED",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public IncidentResponse close(
            @Parameter(description = "Incident identifier", example = "73") @PathVariable long id
    ) {
        long incidentId = closeIncidentService.close(new CloseIncidentCommand(id)).incidentId();
        return currentIncident(incidentId);
    }

    @PostMapping("/{id}/reopen")
    @Operation(
            summary = "Reopen an incident",
            description = "Moves a RESOLVED incident back to IN_PROGRESS when the problem persists."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Incident returned to IN_PROGRESS"),
            @ApiResponse(responseCode = "400", description = "Incident identifier is invalid",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Incident was not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Incident is not RESOLVED",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public IncidentResponse reopen(
            @Parameter(description = "Incident identifier", example = "73") @PathVariable long id
    ) {
        long incidentId = reopenIncidentService.reopen(new ReopenIncidentCommand(id)).incidentId();
        return currentIncident(incidentId);
    }

    @PostMapping("/{id}/cancel")
    @Operation(
            summary = "Cancel an incident",
            description = "Moves an OPEN, ASSIGNED, or IN_PROGRESS incident to CANCELLED. Resolved and closed incidents cannot be cancelled."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Incident moved to CANCELLED"),
            @ApiResponse(responseCode = "400", description = "Incident identifier is invalid",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Incident was not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Current incident status does not allow cancellation",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public IncidentResponse cancel(
            @Parameter(description = "Incident identifier", example = "73") @PathVariable long id
    ) {
        long incidentId = cancelIncidentService.cancel(new CancelIncidentCommand(id)).incidentId();
        return currentIncident(incidentId);
    }

    private IncidentResponse currentIncident(long incidentId) {
        return mapper.toResponse(getIncidentService.get(incidentId));
    }
}
