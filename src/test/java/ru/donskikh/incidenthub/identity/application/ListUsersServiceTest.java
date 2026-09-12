package ru.donskikh.incidenthub.identity.application;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import ru.donskikh.incidenthub.identity.User;
import ru.donskikh.incidenthub.identity.UserRepository;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListUsersServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ListUsersService service;

    @Test
    void listsAllUsersWithStableSortingAndMetadata() {
        User user = user();
        when(userRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(user), PageRequest.of(1, 5), 8));

        ListUsersResult result = service.execute(new ListUsersQuery(1, 5, null));

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(userRepository).findAll(captor.capture());
        assertThat(captor.getValue().getSort().toList()).extracting(Sort.Order::getProperty)
                .containsExactly("displayName", "id");
        assertThat(result.items().getFirst()).isEqualTo(new ListUserItem(
                9L,
                "user@example.com",
                "Example User",
                true,
                Instant.parse("2026-09-01T10:00:00Z"),
                Instant.parse("2026-09-02T11:00:00Z")
        ));
        assertThat(result.page()).isEqualTo(1);
        assertThat(result.size()).isEqualTo(5);
        assertThat(result.totalElements()).isEqualTo(6);
        assertThat(result.totalPages()).isEqualTo(2);
        assertThat(result.hasNext()).isFalse();
        assertThat(result.hasPrevious()).isTrue();
    }

    @Test
    void filtersByActive() {
        when(userRepository.findAllByActive(false, PageRequest.of(
                0, 20, Sort.by(Sort.Order.asc("displayName"), Sort.Order.asc("id"))
        ))).thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        service.execute(new ListUsersQuery(0, 20, false));

        verify(userRepository).findAllByActive(false, PageRequest.of(
                0, 20, Sort.by(Sort.Order.asc("displayName"), Sort.Order.asc("id"))
        ));
    }

    @Test
    void rejectsNullQuery() {
        assertThatThrownBy(() -> service.execute(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("query must not be null");

        verifyNoInteractions(userRepository);
    }

    private static User user() {
        User user = mock(User.class);
        when(user.getId()).thenReturn(9L);
        when(user.getEmail()).thenReturn("user@example.com");
        when(user.getDisplayName()).thenReturn("Example User");
        when(user.isActive()).thenReturn(true);
        when(user.getCreatedAt()).thenReturn(Instant.parse("2026-09-01T10:00:00Z"));
        when(user.getUpdatedAt()).thenReturn(Instant.parse("2026-09-02T11:00:00Z"));
        return user;
    }
}
