package ru.donskikh.incidenthub.auth.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import ru.donskikh.incidenthub.auth.InvalidCredentialsException;
import ru.donskikh.incidenthub.identity.User;
import ru.donskikh.incidenthub.identity.UserRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LoginServiceTest {

    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private AccessTokenIssuer accessTokenIssuer;
    private LoginService service;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        accessTokenIssuer = mock(AccessTokenIssuer.class);
        service = new LoginService(userRepository, passwordEncoder, accessTokenIssuer);
    }

    @Test
    void returnsAccessTokenForValidCredentials() {
        User user = authenticatedUser();
        when(userRepository.findByNormalizedEmail("user@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("correct-password", "$2a$10$password-hash")).thenReturn(true);
        when(accessTokenIssuer.issue(user)).thenReturn(new AccessTokenIssuer.IssuedAccessToken("jwt", 3600));

        LoginResult result = service.login(new LoginCommand("user@example.com", "correct-password"));

        assertThat(result).isEqualTo(new LoginResult("jwt", "Bearer", 3600));
    }

    @Test
    void rejectsWrongPasswordWithGenericMessage() {
        User user = authenticatedUser();
        when(userRepository.findByNormalizedEmail("user@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-password", "$2a$10$password-hash")).thenReturn(false);

        assertInvalidCredentials(new LoginCommand("user@example.com", "wrong-password"));
        verify(accessTokenIssuer, never()).issue(user);
    }

    @Test
    void rejectsUnknownUserWithTheSameGenericMessage() {
        when(userRepository.findByNormalizedEmail("missing@example.com")).thenReturn(Optional.empty());

        assertInvalidCredentials(new LoginCommand("missing@example.com", "wrong-password"));
        verify(passwordEncoder, never()).matches("wrong-password", "$2a$10$password-hash");
    }

    @Test
    void rejectsLegacyUserWithoutPasswordHash() {
        User user = mock(User.class);
        when(user.isActive()).thenReturn(true);
        when(userRepository.findByNormalizedEmail("legacy@example.com")).thenReturn(Optional.of(user));

        assertInvalidCredentials(new LoginCommand("legacy@example.com", "any-password"));
        verify(passwordEncoder, never()).matches("any-password", null);
    }

    private void assertInvalidCredentials(LoginCommand command) {
        assertThatThrownBy(() -> service.login(command))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Invalid email or password");
    }

    private static User authenticatedUser() {
        User user = mock(User.class);
        when(user.isActive()).thenReturn(true);
        when(user.getPasswordHash()).thenReturn("$2a$10$password-hash");
        return user;
    }
}
