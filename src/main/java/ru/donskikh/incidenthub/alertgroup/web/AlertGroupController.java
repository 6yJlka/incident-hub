package ru.donskikh.incidenthub.alertgroup.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.tags.Tag;
import ru.donskikh.incidenthub.alertgroup.AlertGroupStatus;
import ru.donskikh.incidenthub.alertgroup.application.ListAlertGroupsService;
import ru.donskikh.incidenthub.alertgroup.application.GetAlertGroupService;

@RestController
@RequestMapping("/api/v1/alert-groups")
@Tag(name = "Alert groups", description = "Suspected incidents produced by alert correlation")
public class AlertGroupController {

    private final ListAlertGroupsService listService;
    private final GetAlertGroupService getService;
    private final AlertGroupWebMapper mapper;

    public AlertGroupController(
            ListAlertGroupsService listService,
            GetAlertGroupService getService,
            AlertGroupWebMapper mapper
    ) {
        this.listService = listService;
        this.getService = getService;
        this.mapper = mapper;
    }

    @GetMapping
    @Operation(operationId = "listAlertGroups", summary = "List alert groups",
            description = "Returns groups ordered by last alert time and identifier, newest first. "
                    + "Optionally filters by status and probable root service.")
    @ApiResponse(responseCode = "200", description = "Alert group page")
    @ApiResponse(responseCode = "400", description = "Invalid pagination or filters",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    public ListAlertGroupsResponse list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) AlertGroupStatus status,
            @RequestParam(required = false) Long rootServiceId
    ) {
        return mapper.toResponse(listService.execute(mapper.toQuery(page, size, status, rootServiceId)));
    }

    @GetMapping("/{id}")
    @Operation(operationId = "getAlertGroup", summary = "Get an alert group",
            description = "Returns group details and alerts ordered by occurrence time and identifier.")
    @ApiResponse(responseCode = "200", description = "Alert group details and alerts")
    @ApiResponse(responseCode = "404", description = "Alert group not found",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "400", description = "Invalid group identifier",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    public AlertGroupDetailResponse get(@PathVariable long id) {
        return mapper.toResponse(getService.get(mapper.toQuery(id)));
    }
}
