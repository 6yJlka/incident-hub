package ru.donskikh.incidenthub.identity.application;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CreateUserCommandTest {

    @Test
    void acceptsValidCommand() {
        assertThatCode(() -> new CreateUserCommand("user@example.com", "User"))
                .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @MethodSource("invalidCommands")
    void rejectsBlankRequiredFields(String email, String displayName, String message) {
        assertThatThrownBy(() -> new CreateUserCommand(email, displayName))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(message);
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
