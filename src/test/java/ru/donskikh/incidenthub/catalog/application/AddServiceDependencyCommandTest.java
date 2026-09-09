package ru.donskikh.incidenthub.catalog.application;

import org.junit.jupiter.api.Test;
import ru.donskikh.incidenthub.catalog.DependencyType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AddServiceDependencyCommandTest {

    @Test
    void acceptsValidValues() {
        AddServiceDependencyCommand command = new AddServiceDependencyCommand(
                42L, 43L, DependencyType.SYNC
        );

        assertThat(command.dependentServiceId()).isEqualTo(42L);
        assertThat(command.dependencyServiceId()).isEqualTo(43L);
        assertThat(command.type()).isEqualTo(DependencyType.SYNC);
    }

    @Test
    void rejectsNonPositiveDependentServiceId() {
        assertThatThrownBy(() -> command(0L, 43L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("dependentServiceId must be positive");
        assertThatThrownBy(() -> command(-1L, 43L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("dependentServiceId must be positive");
    }

    @Test
    void rejectsNonPositiveDependencyServiceId() {
        assertThatThrownBy(() -> command(42L, 0L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("dependencyServiceId must be positive");
        assertThatThrownBy(() -> command(42L, -1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("dependencyServiceId must be positive");
    }

    @Test
    void rejectsNullDependencyType() {
        assertThatThrownBy(() -> new AddServiceDependencyCommand(42L, 43L, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("type must not be null");
    }

    private static AddServiceDependencyCommand command(long dependentServiceId, long dependencyServiceId) {
        return new AddServiceDependencyCommand(
                dependentServiceId,
                dependencyServiceId,
                DependencyType.SYNC
        );
    }
}
