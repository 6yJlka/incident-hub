package ru.donskikh.incidenthub.auth.application;

import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import ru.donskikh.incidenthub.identity.User;
import ru.donskikh.incidenthub.identity.UserEmailAlreadyExistsException;
import ru.donskikh.incidenthub.identity.UserRepository;
import ru.donskikh.incidenthub.identity.UserRole;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RegisterUserServiceTest {

    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private RegisterUserService service;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        service = new RegisterUserService(userRepository, passwordEncoder);
    }

    @Test
    void registersReporterWithBcryptHash() {
        when(passwordEncoder.encode("secure-password")).thenReturn("$2a$10$encoded-password");
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            ReflectionTestUtils.setField(user, "id", 9L);
            return user;
        });

        RegisterUserResult result = service.register(new RegisterUserCommand(
                "  User@Example.COM  ", "Example User", "secure-password"
        ));

        assertThat(result).isEqualTo(new RegisterUserResult(
                9L, "user@example.com", "Example User", UserRole.REPORTER, true
        ));
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).saveAndFlush(userCaptor.capture());
        assertThat(userCaptor.getValue().getRole()).isEqualTo(UserRole.REPORTER);
        verify(passwordEncoder).encode("secure-password");
    }

    @Test
    void rejectsEmailThatAlreadyExists() {
        when(passwordEncoder.encode("secure-password")).thenReturn("$2a$10$encoded-password");
        when(userRepository.existsByNormalizedEmail("user@example.com")).thenReturn(true);

        assertThatThrownBy(() -> service.register(new RegisterUserCommand(
                "User@Example.COM", "Example User", "secure-password"
        )))
                .isInstanceOf(UserEmailAlreadyExistsException.class)
                .hasMessage("User email already exists: user@example.com");

        verify(userRepository, never()).saveAndFlush(any());
    }

    @Test
    void translatesConcurrentEmailConflictByConstraintName() {
        when(passwordEncoder.encode("secure-password")).thenReturn("$2a$10$encoded-password");
        DataIntegrityViolationException databaseException = databaseException("uk_users_email_lower");
        when(userRepository.saveAndFlush(any(User.class))).thenThrow(databaseException);

        assertThatThrownBy(() -> service.register(new RegisterUserCommand(
                "user@example.com", "Example User", "secure-password"
        )))
                .isInstanceOf(UserEmailAlreadyExistsException.class)
                .hasCause(databaseException);
    }

    @Test
    void rethrowsUnrelatedIntegrityViolation() {
        when(passwordEncoder.encode("secure-password")).thenReturn("$2a$10$encoded-password");
        DataIntegrityViolationException databaseException = databaseException("chk_users_role");
        when(userRepository.saveAndFlush(any(User.class))).thenThrow(databaseException);

        assertThatThrownBy(() -> service.register(new RegisterUserCommand(
                "user@example.com", "Example User", "secure-password"
        ))).isSameAs(databaseException);
    }

    private static DataIntegrityViolationException databaseException(String constraintName) {
        ConstraintViolationException violation = mock(ConstraintViolationException.class);
        when(violation.getConstraintName()).thenReturn(constraintName);
        return new DataIntegrityViolationException("integrity violation", violation);
    }
}
