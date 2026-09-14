package ru.donskikh.incidenthub.auth.web;

import org.springframework.stereotype.Component;
import ru.donskikh.incidenthub.auth.application.LoginCommand;
import ru.donskikh.incidenthub.auth.application.LoginResult;
import ru.donskikh.incidenthub.auth.application.RegisterUserCommand;
import ru.donskikh.incidenthub.auth.application.RegisterUserResult;

@Component
public class AuthWebMapper {

    public RegisterUserCommand toCommand(RegisterRequest request) {
        return new RegisterUserCommand(request.email(), request.displayName(), request.password());
    }

    public LoginCommand toCommand(LoginRequest request) {
        return new LoginCommand(request.email(), request.password());
    }

    public RegisterResponse toResponse(RegisterUserResult result) {
        return new RegisterResponse(
                result.userId(), result.email(), result.displayName(), result.role(), result.active()
        );
    }

    public LoginResponse toResponse(LoginResult result) {
        return new LoginResponse(result.accessToken(), result.tokenType(), result.expiresInSeconds());
    }
}
