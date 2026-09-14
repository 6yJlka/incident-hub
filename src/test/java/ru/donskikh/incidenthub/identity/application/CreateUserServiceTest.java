package ru.donskikh.incidenthub.identity.application;

import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
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
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateUserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private CreateUserService service;

    @Test
    void createsUserWithNormalizedEmail() {
        when(passwordEncoder.encode("secure-password")).thenReturn("$2a$10$encoded-password");
        User savedUser = mock(User.class);
        when(userRepository.existsByNormalizedEmail("user@example.com")).thenReturn(false);
        when(userRepository.saveAndFlush(any(User.class))).thenReturn(savedUser);
        when(savedUser.getId()).thenReturn(9L);
        when(savedUser.getEmail()).thenReturn("user@example.com");
        when(savedUser.getDisplayName()).thenReturn("Example User");
        when(savedUser.getRole()).thenReturn(UserRole.ENGINEER);
        when(savedUser.isActive()).thenReturn(true);

        CreateUserResult result = service.create(command("  User@Example.COM  ", "Example User"));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getEmail()).isEqualTo("user@example.com");
        assertThat(captor.getValue().getDisplayName()).isEqualTo("Example User");
        assertThat(captor.getValue().getPasswordHash()).isEqualTo("$2a$10$encoded-password");
        assertThat(captor.getValue().getRole()).isEqualTo(UserRole.ENGINEER);
        assertThat(result).isEqualTo(new CreateUserResult(
                9L, "user@example.com", "Example User", UserRole.ENGINEER, true
        ));
        verify(passwordEncoder).encode("secure-password");
    }

    @Test
    void throwsDomainExceptionWhenNormalizedEmailAlreadyExists() {
        when(passwordEncoder.encode("secure-password")).thenReturn("$2a$10$encoded-password");
        when(userRepository.existsByNormalizedEmail("user@example.com")).thenReturn(true);

        assertThatThrownBy(() -> service.create(command("USER@example.com", "User")))
                .isInstanceOf(UserEmailAlreadyExistsException.class)
                .hasMessage("User email already exists: user@example.com");

        verify(userRepository, never()).saveAndFlush(any(User.class));
    }

    @Test
    void translatesConcurrentEmailConflictToDomainException() {
        when(passwordEncoder.encode("secure-password")).thenReturn("$2a$10$encoded-password");
        DataIntegrityViolationException databaseException = databaseException("uk_users_email_lower");
        when(userRepository.existsByNormalizedEmail("user@example.com")).thenReturn(false);
        when(userRepository.saveAndFlush(any(User.class))).thenThrow(databaseException);

        assertThatThrownBy(() -> service.create(command("USER@example.com", "User")))
                .isInstanceOf(UserEmailAlreadyExistsException.class)
                .hasMessage("User email already exists: user@example.com")
                .hasCause(databaseException);
    }

    @Test
    void doesNotTranslateDifferentIntegrityConstraint() {
        when(passwordEncoder.encode("secure-password")).thenReturn("$2a$10$encoded-password");
        DataIntegrityViolationException databaseException = databaseException("chk_users_email_not_blank");
        when(userRepository.existsByNormalizedEmail("user@example.com")).thenReturn(false);
        when(userRepository.saveAndFlush(any(User.class))).thenThrow(databaseException);

        assertThatThrownBy(() -> service.create(command("user@example.com", "User")))
                .isSameAs(databaseException);
    }

    @Test
    void doesNotTranslateIntegrityViolationWithoutConstraintName() {
        when(passwordEncoder.encode("secure-password")).thenReturn("$2a$10$encoded-password");
        DataIntegrityViolationException databaseException = databaseException(null);
        when(userRepository.existsByNormalizedEmail("user@example.com")).thenReturn(false);
        when(userRepository.saveAndFlush(any(User.class))).thenThrow(databaseException);

        assertThatThrownBy(() -> service.create(command("user@example.com", "User")))
                .isSameAs(databaseException);
    }

    @Test
    void rejectsNullCommand() {
        assertThatThrownBy(() -> service.create(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("command must not be null");

        verifyNoInteractions(userRepository, passwordEncoder);
    }

    private static CreateUserCommand command(String email, String displayName) {
        return new CreateUserCommand(email, displayName, "secure-password", UserRole.ENGINEER);
    }

    private static DataIntegrityViolationException databaseException(String constraintName) {
        ConstraintViolationException violation = mock(ConstraintViolationException.class);
        when(violation.getConstraintName()).thenReturn(constraintName);
        return new DataIntegrityViolationException("integrity violation", violation);
    }
}
