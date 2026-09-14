package ru.donskikh.incidenthub.auth.application;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.donskikh.incidenthub.auth.InvalidCredentialsException;
import ru.donskikh.incidenthub.identity.User;
import ru.donskikh.incidenthub.identity.UserRepository;

import java.util.Objects;

@Service
public class LoginService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AccessTokenIssuer accessTokenIssuer;

    public LoginService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AccessTokenIssuer accessTokenIssuer
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.accessTokenIssuer = accessTokenIssuer;
    }

    @Transactional(readOnly = true)
    public LoginResult login(LoginCommand command) {
        Objects.requireNonNull(command, "command must not be null");

        User user = userRepository.findByNormalizedEmail(command.email())
                .filter(User::isActive)
                .filter(candidate -> candidate.getPasswordHash() != null)
                .filter(candidate -> passwordEncoder.matches(command.password(), candidate.getPasswordHash()))
                .orElseThrow(InvalidCredentialsException::new);

        AccessTokenIssuer.IssuedAccessToken token = accessTokenIssuer.issue(user);
        return new LoginResult(token.value(), "Bearer", token.expiresInSeconds());
    }
}
