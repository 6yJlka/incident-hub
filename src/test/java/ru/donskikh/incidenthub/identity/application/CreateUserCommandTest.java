package ru.donskikh.incidenthub.identity.application;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import ru.donskikh.incidenthub.identity.UserRole;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CreateUserCommandTest {

    @Test
    void acceptsValidCommand() {
        assertThatCode(() -> new CreateUserCommand(
                "user@example.com", "User", "secure-password", UserRole.ENGINEER
        ))
                .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @MethodSource("invalidCommands")
    void rejectsBlankRequiredFields(String email, String displayName, String message) {
        assertThatThrownBy(() -> new CreateUserCommand(
                email, displayName, "secure-password", UserRole.ENGINEER
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(message);
    }

    @Test
    void rejectsBlankPassword() {
        assertThatThrownBy(() -> new CreateUserCommand(
                "user@example.com", "User", "  ", UserRole.ENGINEER
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("password must not be blank");
    }

    @Test
    void rejectsMissingRole() {
        assertThatThrownBy(() -> new CreateUserCommand(
                "user@example.com", "User", "secure-password", null
        ))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("role must not be null");
    }

    private static Stream<Arguments> invalidCommands() {
        return Stream.of(
                Arguments.of(null, "User", "email must not be blank"),
                Arguments.of("  ", "User", "email must not be blank"),
                Arguments.of("user@example.com", null, "displayName must not be blank"),
                Arguments.of("user@example.com", "  ", "displayName must not be blank")
        );
    }
}
