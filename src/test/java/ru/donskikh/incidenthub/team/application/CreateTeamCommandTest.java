package ru.donskikh.incidenthub.team.application;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CreateTeamCommandTest {

    @Test
    void acceptsValidCommand() {
        assertThatCode(() -> new CreateTeamCommand("platform", "Platform"))
                .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @MethodSource("invalidCommands")
    void rejectsBlankRequiredFields(String code, String name, String message) {
        assertThatThrownBy(() -> new CreateTeamCommand(code, name))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(message);
    }

    private static Stream<Arguments> invalidCommands() {
        return Stream.of(
                Arguments.of(null, "Platform", "code must not be blank"),
                Arguments.of("  ", "Platform", "code must not be blank"),
                Arguments.of("platform", null, "name must not be blank"),
                Arguments.of("platform", "  ", "name must not be blank")
        );
    }
}
