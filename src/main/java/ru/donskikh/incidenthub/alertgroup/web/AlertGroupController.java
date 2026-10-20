package ru.donskikh.incidenthub.alertgroup.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.donskikh.incidenthub.alertgroup.AlertGroupStatus;
import ru.donskikh.incidenthub.alertgroup.application.ConfirmAlertGroupService;
import ru.donskikh.incidenthub.alertgroup.application.DismissAlertGroupService;
import ru.donskikh.incidenthub.alertgroup.application.GetAlertGroupService;
import ru.donskikh.incidenthub.alertgroup.application.ListAlertGroupsService;
import ru.donskikh.incidenthub.security.AuthenticatedUser;

@RestController
@RequestMapping("/api/v1/alert-groups")
@Tag(name = "Alert groups", description = "Suspected incidents produced by alert correlation")
public class AlertGroupController {

    private final ListAlertGroupsService listService;
    private final GetAlertGroupService getService;
    private final AlertGroupWebMapper mapper;
    private final ConfirmAlertGroupService confirmService;
    private final DismissAlertGroupService dismissService;

    public AlertGroupController(
            ListAlertGroupsService listService,
            GetAlertGroupService getService,
            AlertGroupWebMapper mapper,
            ConfirmAlertGroupService confirmService,
            DismissAlertGroupService dismissService
    ) {
        this.listService = listService;
        this.getService = getService;
        this.mapper = mapper;
        this.confirmService = confirmService;
        this.dismissService = dismissService;
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
            description = "Returns group details, the version required for decisions, and alerts ordered by occurrence time and identifier.")
    @ApiResponse(responseCode = "200", description = "Alert group details and alerts")
    @ApiResponse(responseCode = "404", description = "Alert group not found",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "400", description = "Invalid group identifier",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    public AlertGroupDetailResponse get(@PathVariable long id, @AuthenticationPrincipal AuthenticatedUser user) {
        return mapper.toResponse(getService.get(mapper.toQuery(id)), user);
    }

    @PostMapping("/{id}/confirm")
    @PreAuthorize("@incidentActionAuthorization.isAllowed(principal, T(ru.donskikh.incidenthub.alertgroup.AlertGroupAction).CONFIRM)")
    @Operation(operationId = "confirmAlertGroup", summary = "Confirm an alert group",
            description = "Creates an AUTOMATIC incident and confirms the OPEN group atomically. "
                    + "Severity and priority are supplied by the engineer. expectedVersion must match the viewed group version. "
                    + "businessServiceId is required for UNDETERMINED assessments; otherwise returns "
                    + "urn:incident-hub:problem:root-service-required (400).")
    @ApiResponse(responseCode = "200", description = "Confirmed group with incident")
    @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "Group or service not found", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "Group is not OPEN, or changed concurrently (distinct problem types)", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    public AlertGroupDetailResponse confirm(@PathVariable long id, @Valid @RequestBody ConfirmAlertGroupRequest request,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return mapper.toResponse(confirmService.confirm(mapper.toCommand(id, request, user.userId())), user);
    }

    @PostMapping("/{id}/dismiss")
    @PreAuthorize("@incidentActionAuthorization.isAllowed(principal, T(ru.donskikh.incidenthub.alertgroup.AlertGroupAction).DISMISS)")
    @Operation(operationId = "dismissAlertGroup", summary = "Dismiss an alert group",
            description = "Dismisses an OPEN group without creating an incident. A reason is optional; expectedVersion must match the viewed group version.")
    @ApiResponse(responseCode = "200", description = "Dismissed group")
    @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "Group not found", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "Group is not OPEN, or changed concurrently (distinct problem types)", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    public AlertGroupDetailResponse dismiss(@PathVariable long id,
            @Valid @RequestBody DismissAlertGroupRequest request,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return mapper.toResponse(dismissService.dismiss(mapper.toCommand(id, request, user.userId())), user);
    }
}
