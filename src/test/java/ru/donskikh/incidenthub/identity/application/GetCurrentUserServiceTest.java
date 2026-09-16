package ru.donskikh.incidenthub.identity.application;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.donskikh.incidenthub.identity.User;
import ru.donskikh.incidenthub.identity.UserNotFoundException;
import ru.donskikh.incidenthub.identity.UserRepository;
import ru.donskikh.incidenthub.identity.UserRole;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetCurrentUserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private GetCurrentUserService service;

    @Test
    void returnsCurrentUser() {
        User user = mock(User.class);
        when(user.getId()).thenReturn(42L);
        when(user.getEmail()).thenReturn("engineer@example.com");
        when(user.getDisplayName()).thenReturn("Elena Sokolova");
        when(user.getRole()).thenReturn(UserRole.ENGINEER);
        when(user.isActive()).thenReturn(true);
        when(userRepository.findById(42L)).thenReturn(Optional.of(user));

        GetCurrentUserResult result = service.get(42L);

        assertThat(result).isEqualTo(new GetCurrentUserResult(
                42L,
                "engineer@example.com",
                "Elena Sokolova",
                UserRole.ENGINEER,
                true
        ));
    }

    @Test
    void reportsMissingCurrentUser() {
        when(userRepository.findById(42L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(42L))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("User not found: 42");

        verify(userRepository).findById(42L);
    }
}
