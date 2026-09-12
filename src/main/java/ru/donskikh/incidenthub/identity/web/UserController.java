package ru.donskikh.incidenthub.identity.web;

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
import ru.donskikh.incidenthub.identity.application.CreateUserService;
import ru.donskikh.incidenthub.identity.application.ListUsersService;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "Users", description = "Create incident reporters and assignees and browse them")
public class UserController {

    private final CreateUserService createUserService;
    private final ListUsersService listUsersService;
    private final UserWebMapper mapper;

    public UserController(
            CreateUserService createUserService,
            ListUsersService listUsersService,
            UserWebMapper mapper
    ) {
        this.createUserService = createUserService;
        this.listUsersService = listUsersService;
        this.mapper = mapper;
    }

    @PostMapping
    @Operation(
            summary = "Create a user",
            description = "Creates an active user. The normalized email must be unique. Passwords are outside this API."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "User created"),
            @ApiResponse(responseCode = "400", description = "Request validation failed",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Normalized email already exists",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<CreateUserResponse> create(@Valid @RequestBody CreateUserRequest request) {
        CreateUserResponse response = mapper.toResponse(createUserService.create(mapper.toCommand(request)));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.userId())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping
    @Operation(
            summary = "List users",
            description = "Returns users ordered by display name and identifier, optionally filtered by activity."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Page of users returned"),
            @ApiResponse(responseCode = "400", description = "Pagination or filter parameter is invalid",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ListUsersResponse list(
            @Parameter(description = "Zero-based page index", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size from 1 to 100", example = "20")
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Filter by active or inactive users", example = "true")
            @RequestParam(required = false) Boolean active
    ) {
        return mapper.toResponse(listUsersService.execute(mapper.toQuery(page, size, active)));
    }
}
