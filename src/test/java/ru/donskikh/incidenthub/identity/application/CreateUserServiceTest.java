package ru.donskikh.incidenthub.identity.application;

import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import ru.donskikh.incidenthub.identity.User;
import ru.donskikh.incidenthub.identity.UserEmailAlreadyExistsException;
import ru.donskikh.incidenthub.identity.UserRepository;

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

    @InjectMocks
    private CreateUserService service;

    @Test
    void createsUserWithNormalizedEmail() {
        User savedUser = mock(User.class);
        when(userRepository.existsByNormalizedEmail("user@example.com")).thenReturn(false);
        when(userRepository.saveAndFlush(any(User.class))).thenReturn(savedUser);
        when(savedUser.getId()).thenReturn(9L);
        when(savedUser.getEmail()).thenReturn("user@example.com");
        when(savedUser.isActive()).thenReturn(true);

        CreateUserResult result = service.create(new CreateUserCommand(
                "  User@Example.COM  ",
                "Example User"
        ));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getEmail()).isEqualTo("user@example.com");
        assertThat(captor.getValue().getDisplayName()).isEqualTo("Example User");
        assertThat(result).isEqualTo(new CreateUserResult(9L, "user@example.com", true));
    }

    @Test
    void throwsDomainExceptionWhenNormalizedEmailAlreadyExists() {
        when(userRepository.existsByNormalizedEmail("user@example.com")).thenReturn(true);

        assertThatThrownBy(() -> service.create(new CreateUserCommand("USER@example.com", "User")))
                .isInstanceOf(UserEmailAlreadyExistsException.class)
                .hasMessage("User email already exists: user@example.com");

        verify(userRepository, never()).saveAndFlush(any(User.class));
    }

    @Test
    void translatesConcurrentEmailConflictToDomainException() {
        DataIntegrityViolationException databaseException = databaseException("uk_users_email_lower");
        when(userRepository.existsByNormalizedEmail("user@example.com")).thenReturn(false);
        when(userRepository.saveAndFlush(any(User.class))).thenThrow(databaseException);

        assertThatThrownBy(() -> service.create(new CreateUserCommand("USER@example.com", "User")))
                .isInstanceOf(UserEmailAlreadyExistsException.class)
                .hasMessage("User email already exists: user@example.com")
                .hasCause(databaseException);
    }

    @Test
    void doesNotTranslateDifferentIntegrityConstraint() {
        DataIntegrityViolationException databaseException = databaseException("chk_users_email_not_blank");
        when(userRepository.existsByNormalizedEmail("user@example.com")).thenReturn(false);
        when(userRepository.saveAndFlush(any(User.class))).thenThrow(databaseException);

        assertThatThrownBy(() -> service.create(new CreateUserCommand("user@example.com", "User")))
                .isSameAs(databaseException);
    }

    @Test
    void doesNotTranslateIntegrityViolationWithoutConstraintName() {
        DataIntegrityViolationException databaseException = databaseException(null);
        when(userRepository.existsByNormalizedEmail("user@example.com")).thenReturn(false);
        when(userRepository.saveAndFlush(any(User.class))).thenThrow(databaseException);

        assertThatThrownBy(() -> service.create(new CreateUserCommand("user@example.com", "User")))
                .isSameAs(databaseException);
    }

    @Test
    void rejectsNullCommand() {
        assertThatThrownBy(() -> service.create(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("command must not be null");

        verifyNoInteractions(userRepository);
    }

    private static DataIntegrityViolationException databaseException(String constraintName) {
        ConstraintViolationException violation = mock(ConstraintViolationException.class);
        when(violation.getConstraintName()).thenReturn(constraintName);
        return new DataIntegrityViolationException("integrity violation", violation);
    }
}
