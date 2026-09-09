package ru.donskikh.incidenthub.catalog.application;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GetAffectedServicesQueryTest {

    @Test
    void acceptsValidBusinessServiceIdAndMaximumDepth() {
        GetAffectedServicesQuery query = new GetAffectedServicesQuery(
                42L,
                GetAffectedServicesQuery.MAX_DEPTH
        );

        assertThat(query.businessServiceId()).isEqualTo(42L);
        assertThat(query.maxDepth()).isEqualTo(10);
    }

    @Test
    void rejectsNonPositiveBusinessServiceId() {
        assertThatThrownBy(() -> new GetAffectedServicesQuery(0L, 3))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("businessServiceId must be positive");
        assertThatThrownBy(() -> new GetAffectedServicesQuery(-1L, 3))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("businessServiceId must be positive");
    }

    @Test
    void rejectsNonPositiveMaxDepth() {
        assertThatThrownBy(() -> new GetAffectedServicesQuery(42L, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("maxDepth must be greater than 0");
        assertThatThrownBy(() -> new GetAffectedServicesQuery(42L, -1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("maxDepth must be greater than 0");
    }

    @Test
    void rejectsMaxDepthAboveLimit() {
        assertThatThrownBy(() -> new GetAffectedServicesQuery(42L, 11))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("maxDepth must not exceed 10");
    }
}
