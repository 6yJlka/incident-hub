package ru.donskikh.incidenthub.team.web;

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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import ru.donskikh.incidenthub.team.application.CreateTeamService;
import ru.donskikh.incidenthub.team.application.ListTeamsService;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/teams")
@Tag(name = "Teams", description = "Create service-owning teams and browse them")
public class TeamController {

    private final CreateTeamService createTeamService;
    private final ListTeamsService listTeamsService;
    private final TeamWebMapper mapper;

    public TeamController(
            CreateTeamService createTeamService,
            ListTeamsService listTeamsService,
            TeamWebMapper mapper
    ) {
        this.createTeamService = createTeamService;
        this.listTeamsService = listTeamsService;
        this.mapper = mapper;
    }

    @PostMapping
    @Operation(
            summary = "Create a team",
            description = "Creates an active team. The normalized code must be unique."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Team created"),
            @ApiResponse(responseCode = "400", description = "Request validation failed",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Normalized team code already exists",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<CreateTeamResponse> create(@Valid @RequestBody CreateTeamRequest request) {
        CreateTeamResponse response = mapper.toResponse(createTeamService.create(mapper.toCommand(request)));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.teamId())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping
    @Operation(
            summary = "List teams",
            description = "Returns teams ordered by name and identifier, optionally filtered by activity."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Page of teams returned"),
            @ApiResponse(responseCode = "400", description = "Pagination or filter parameter is invalid",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ListTeamsResponse list(
            @Parameter(description = "Zero-based page index", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size from 1 to 100", example = "20")
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Filter by active or inactive teams", example = "true")
            @RequestParam(required = false) Boolean active
    ) {
        return mapper.toResponse(listTeamsService.execute(mapper.toQuery(page, size, active)));
    }
}
