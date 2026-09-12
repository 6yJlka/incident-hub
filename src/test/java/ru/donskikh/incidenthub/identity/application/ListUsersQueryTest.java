package ru.donskikh.incidenthub.identity.application;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ListUsersQueryTest {

    @Test
    void acceptsBoundaryValues() {
        assertThatCode(() -> new ListUsersQuery(0, 100, null)).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @MethodSource("invalidQueries")
    void rejectsInvalidPagination(int page, int size, String message) {
        assertThatThrownBy(() -> new ListUsersQuery(page, size, true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(message);
    }

    private static Stream<Arguments> invalidQueries() {
        return Stream.of(
                Arguments.of(-1, 20, "page must be greater than or equal to 0"),
                Arguments.of(0, 0, "size must be greater than 0"),
                Arguments.of(0, -1, "size must be greater than 0"),
                Arguments.of(0, 101, "size must not exceed 100")
        );
    }
}
