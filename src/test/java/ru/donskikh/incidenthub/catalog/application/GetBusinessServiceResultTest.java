package ru.donskikh.incidenthub.catalog.application;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GetBusinessServiceResultTest {

    @Test
    void copiesDependencyList() {
        ArrayList<DirectServiceDependencyItem> dependencies = new ArrayList<>();
        GetBusinessServiceResult result = new GetBusinessServiceResult(
                42L, "BILLING", "Billing", "Billing service",
                7L, "Platform", "PLATFORM", null, true,
                null, null, dependencies
        );

        dependencies.add(null);

        assertThat(result.dependencies()).isEmpty();
        assertThatThrownBy(() -> result.dependencies().add(null))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void rejectsNullDependencyList() {
        assertThatThrownBy(() -> new GetBusinessServiceResult(
                42L, "BILLING", "Billing", "Billing service",
                7L, "Platform", "PLATFORM", null, true,
                null, null, null
        ))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("dependencies must not be null");
    }
}
