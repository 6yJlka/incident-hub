package ru.donskikh.incidenthub.identity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserTest {

    @Test
    void createsActiveUser() {
        User user = new User("user@example.com", "User Name");

        assertThat(user.isActive()).isTrue();
        assertThat(user.getEmail()).isEqualTo("user@example.com");
        assertThat(user.getDisplayName()).isEqualTo("User Name");
    }

    @Test
    void normalizesEmail() {
        User user = new User("  USER@Example.COM  ", "User Name");

        assertThat(user.getEmail()).isEqualTo("user@example.com");
    }

    @Test
    void rejectsBlankEmail() {
        assertThatThrownBy(() -> new User("   ", "User Name"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("email must not be blank");
    }

    @Test
    void rejectsBlankDisplayName() {
        assertThatThrownBy(() -> new User("user@example.com", "   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("displayName must not be blank");
    }

    @Test
    void activatesAndDeactivatesUser() {
        User user = new User("user@example.com", "User Name");

        user.deactivate();
        assertThat(user.isActive()).isFalse();

        user.activate();
        assertThat(user.isActive()).isTrue();
    }
}
