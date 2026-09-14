package ru.donskikh.incidenthub.auth.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import ru.donskikh.incidenthub.auth.application.LoginService;
import ru.donskikh.incidenthub.auth.application.RegisterUserService;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "Register users and obtain access tokens")
@SecurityRequirements
public class AuthController {

    private final RegisterUserService registerUserService;
    private final LoginService loginService;
    private final AuthWebMapper mapper;

    public AuthController(RegisterUserService registerUserService, LoginService loginService, AuthWebMapper mapper) {
        this.registerUserService = registerUserService;
        this.loginService = loginService;
        this.mapper = mapper;
    }

    @PostMapping("/register")
    @Operation(summary = "Register a user", description = "Creates an active user with the REPORTER role")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "User registered"),
            @ApiResponse(responseCode = "400", description = "Request validation failed"),
            @ApiResponse(responseCode = "409", description = "Normalized email already exists")
    })
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        RegisterResponse response = mapper.toResponse(registerUserService.register(mapper.toCommand(request)));
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/v1/users/{id}")
                .buildAndExpand(response.userId())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @PostMapping("/login")
    @Operation(summary = "Log in", description = "Returns a signed JWT access token")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Authentication successful"),
            @ApiResponse(responseCode = "400", description = "Request validation failed"),
            @ApiResponse(responseCode = "401", description = "Invalid email or password")
    })
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return mapper.toResponse(loginService.login(mapper.toCommand(request)));
    }
}
