package ru.donskikh.incidenthub.catalog.application;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GetBusinessServiceQueryTest {

    @Test
    void acceptsPositiveBusinessServiceId() {
        GetBusinessServiceQuery query = new GetBusinessServiceQuery(42L);

        assertThat(query.businessServiceId()).isEqualTo(42L);
    }

    @Test
    void rejectsNonPositiveBusinessServiceId() {
        assertThatThrownBy(() -> new GetBusinessServiceQuery(0L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("businessServiceId must be positive");
        assertThatThrownBy(() -> new GetBusinessServiceQuery(-1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("businessServiceId must be positive");
    }
}
