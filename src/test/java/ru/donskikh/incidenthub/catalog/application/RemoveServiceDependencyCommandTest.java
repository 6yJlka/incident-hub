package ru.donskikh.incidenthub.catalog.application;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RemoveServiceDependencyCommandTest {

    @Test
    void acceptsValidValues() {
        RemoveServiceDependencyCommand command = new RemoveServiceDependencyCommand(42L, 43L);

        assertThat(command.dependentServiceId()).isEqualTo(42L);
        assertThat(command.dependencyServiceId()).isEqualTo(43L);
    }

    @Test
    void rejectsNonPositiveDependentServiceId() {
        assertThatThrownBy(() -> new RemoveServiceDependencyCommand(0L, 43L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("dependentServiceId must be positive");
        assertThatThrownBy(() -> new RemoveServiceDependencyCommand(-1L, 43L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("dependentServiceId must be positive");
    }

    @Test
    void rejectsNonPositiveDependencyServiceId() {
        assertThatThrownBy(() -> new RemoveServiceDependencyCommand(42L, 0L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("dependencyServiceId must be positive");
        assertThatThrownBy(() -> new RemoveServiceDependencyCommand(42L, -1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("dependencyServiceId must be positive");
    }
}
