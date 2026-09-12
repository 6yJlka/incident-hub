package ru.donskikh.incidenthub.catalog.web;

import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import ru.donskikh.incidenthub.catalog.ServiceTier;
import ru.donskikh.incidenthub.catalog.application.AddServiceDependencyService;
import ru.donskikh.incidenthub.catalog.application.CreateBusinessServiceService;
import ru.donskikh.incidenthub.catalog.application.GetAffectedServicesService;
import ru.donskikh.incidenthub.catalog.application.GetBusinessServiceService;
import ru.donskikh.incidenthub.catalog.application.ListBusinessServicesService;
import ru.donskikh.incidenthub.catalog.application.RemoveServiceDependencyService;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/services")
@Tag(name = "Service catalog", description = "Manage business services and their dependency graph")
public class BusinessServiceController {

    private static final String DEFAULT_MAX_DEPTH = "10";

    private final CreateBusinessServiceService createBusinessServiceService;
    private final ListBusinessServicesService listBusinessServicesService;
    private final GetBusinessServiceService getBusinessServiceService;
    private final GetAffectedServicesService getAffectedServicesService;
    private final AddServiceDependencyService addServiceDependencyService;
    private final RemoveServiceDependencyService removeServiceDependencyService;
    private final BusinessServiceWebMapper mapper;

    public BusinessServiceController(
            CreateBusinessServiceService createBusinessServiceService,
            ListBusinessServicesService listBusinessServicesService,
            GetBusinessServiceService getBusinessServiceService,
            GetAffectedServicesService getAffectedServicesService,
            AddServiceDependencyService addServiceDependencyService,
            RemoveServiceDependencyService removeServiceDependencyService,
            BusinessServiceWebMapper mapper
    ) {
        this.createBusinessServiceService = createBusinessServiceService;
        this.listBusinessServicesService = listBusinessServicesService;
        this.getBusinessServiceService = getBusinessServiceService;
        this.getAffectedServicesService = getAffectedServicesService;
        this.addServiceDependencyService = addServiceDependencyService;
        this.removeServiceDependencyService = removeServiceDependencyService;
        this.mapper = mapper;
    }

    @PostMapping
    @Operation(
            summary = "Create a business service",
            description = "Creates an active catalog service owned by an existing team. The normalized code must be unique."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Business service created"),
            @ApiResponse(responseCode = "400", description = "Request validation failed",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Owner team was not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Normalized service code already exists",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<CreateBusinessServiceResponse> create(
            @Valid @RequestBody CreateBusinessServiceRequest request
    ) {
        CreateBusinessServiceResponse response = mapper.toResponse(
                createBusinessServiceService.create(mapper.toCommand(request))
        );
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.businessServiceId())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping
    @Operation(
            summary = "List business services",
            description = "Returns services ordered by name and identifier with optional owner, tier, and activity filters."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Page of services returned"),
            @ApiResponse(responseCode = "400", description = "Pagination or filter parameter is invalid",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ListBusinessServicesResponse list(
            @Parameter(description = "Zero-based page index", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size from 1 to 100", example = "20")
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Filter by owner team identifier", example = "12")
            @RequestParam(required = false) Long ownerTeamId,
            @Parameter(description = "Filter by business criticality tier", example = "TIER_1")
            @RequestParam(required = false) ServiceTier tier,
            @Parameter(description = "Filter by active or inactive services", example = "true")
            @RequestParam(required = false) Boolean active
    ) {
        return mapper.toResponse(listBusinessServicesService.execute(
                mapper.toQuery(page, size, ownerTeamId, tier, active)
        ));
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Get a business service",
            description = "Returns service details together with its direct dependencies."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Business service returned"),
            @ApiResponse(responseCode = "400", description = "Service identifier is invalid",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Business service was not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public BusinessServiceResponse get(
            @Parameter(description = "Business service identifier", example = "42") @PathVariable long id
    ) {
        return mapper.toResponse(getBusinessServiceService.get(mapper.toGetQuery(id)));
    }

    @GetMapping("/{id}/affected")
    @Operation(
            summary = "Find affected services",
            description = "Traverses reverse dependencies and returns each affected service once at its minimum depth."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Affected services returned"),
            @ApiResponse(responseCode = "400", description = "Service identifier or maximum depth is invalid",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Root business service was not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public GetAffectedServicesResponse affected(
            @Parameter(description = "Failed root service identifier", example = "41") @PathVariable long id,
            @Parameter(description = "Maximum number of dependency levels to traverse, from 1 to 10", example = "3")
            @RequestParam(defaultValue = DEFAULT_MAX_DEPTH) int maxDepth
    ) {
        return mapper.toResponse(getAffectedServicesService.get(
                mapper.toAffectedQuery(id, maxDepth)
        ));
    }

    @PostMapping("/{id}/dependencies")
    @Operation(
            summary = "Add a service dependency",
            description = "Declares that the service in the path depends on another existing service. Self-dependencies and duplicate pairs are rejected."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Dependency relationship created"),
            @ApiResponse(responseCode = "400", description = "Service identifier, dependency identifier, or type is invalid",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Dependent or required service was not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Dependency already exists or points to the same service",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<AddServiceDependencyResponse> addDependency(
            @Parameter(description = "Identifier of the service that depends on another service", example = "42")
            @PathVariable long id,
            @Valid @RequestBody AddServiceDependencyRequest request
    ) {
        AddServiceDependencyResponse response = mapper.toResponse(
                addServiceDependencyService.add(mapper.toCommand(id, request))
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/{id}/dependencies/{dependencyId}")
    @Operation(
            summary = "Remove a service dependency",
            description = "Removes the direct dependency from the service in the path to the specified required service."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Dependency relationship removed"),
            @ApiResponse(responseCode = "400", description = "Service or dependency identifier is invalid",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Service or dependency relationship was not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<Void> removeDependency(
            @Parameter(description = "Identifier of the dependent service", example = "42") @PathVariable long id,
            @Parameter(description = "Identifier of the required service", example = "41")
            @PathVariable long dependencyId
    ) {
        removeServiceDependencyService.remove(mapper.toRemoveCommand(id, dependencyId));
        return ResponseEntity.noContent().build();
    }
}
