package ru.donskikh.incidenthub.incident.web;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
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
    public ListIncidentsResponse list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) IncidentStatus status,
            @RequestParam(required = false) IncidentPriority priority,
            @RequestParam(required = false) IncidentSeverity severity,
            @RequestParam(required = false) IncidentSource source,
            @RequestParam(required = false) Long affectedServiceId,
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
    public IncidentResponse get(@PathVariable long id) {
        return mapper.toResponse(getIncidentService.get(id));
    }

    @GetMapping("/{id}/history")
    public IncidentHistoryResponse history(@PathVariable long id) {
        return mapper.toResponse(getIncidentHistoryService.get(id));
    }

    @PostMapping("/{id}/assign")
    public IncidentResponse assign(
            @PathVariable long id,
            @Valid @RequestBody AssignIncidentRequest request
    ) {
        long incidentId = assignIncidentService.assign(mapper.toCommand(id, request)).incidentId();
        return currentIncident(incidentId);
    }

    @PostMapping("/{id}/start")
    public IncidentResponse start(@PathVariable long id) {
        long incidentId = startIncidentProgressService.start(new StartIncidentProgressCommand(id)).incidentId();
        return currentIncident(incidentId);
    }

    @PostMapping("/{id}/resolve")
    public IncidentResponse resolve(@PathVariable long id) {
        long incidentId = resolveIncidentService.resolve(new ResolveIncidentCommand(id)).incidentId();
        return currentIncident(incidentId);
    }

    @PostMapping("/{id}/close")
    public IncidentResponse close(@PathVariable long id) {
        long incidentId = closeIncidentService.close(new CloseIncidentCommand(id)).incidentId();
        return currentIncident(incidentId);
    }

    @PostMapping("/{id}/reopen")
    public IncidentResponse reopen(@PathVariable long id) {
        long incidentId = reopenIncidentService.reopen(new ReopenIncidentCommand(id)).incidentId();
        return currentIncident(incidentId);
    }

    @PostMapping("/{id}/cancel")
    public IncidentResponse cancel(@PathVariable long id) {
        long incidentId = cancelIncidentService.cancel(new CancelIncidentCommand(id)).incidentId();
        return currentIncident(incidentId);
    }

    private IncidentResponse currentIncident(long incidentId) {
        return mapper.toResponse(getIncidentService.get(incidentId));
    }
}
