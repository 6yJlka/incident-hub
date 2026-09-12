package ru.donskikh.incidenthub.identity.web;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<CreateUserResponse> create(@Valid @RequestBody CreateUserRequest request) {
        CreateUserResponse response = mapper.toResponse(createUserService.create(mapper.toCommand(request)));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.userId())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping
    public ListUsersResponse list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Boolean active
    ) {
        return mapper.toResponse(listUsersService.execute(mapper.toQuery(page, size, active)));
    }
}
