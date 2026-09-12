package ru.donskikh.incidenthub.team.web;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<CreateTeamResponse> create(@Valid @RequestBody CreateTeamRequest request) {
        CreateTeamResponse response = mapper.toResponse(createTeamService.create(mapper.toCommand(request)));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.teamId())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping
    public ListTeamsResponse list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Boolean active
    ) {
        return mapper.toResponse(listTeamsService.execute(mapper.toQuery(page, size, active)));
    }
}
