package ru.donskikh.incidenthub.catalog.application;

import org.junit.jupiter.api.Test;
import ru.donskikh.incidenthub.catalog.ServiceTier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ListBusinessServicesQueryTest {

    @Test
    void acceptsValidPageSizeAndFilters() {
        ListBusinessServicesQuery query = new ListBusinessServicesQuery(
                2, 100, 7L, ServiceTier.TIER_1, true
        );

        assertThat(query.page()).isEqualTo(2);
        assertThat(query.size()).isEqualTo(100);
        assertThat(query.ownerTeamId()).isEqualTo(7L);
        assertThat(query.tier()).isEqualTo(ServiceTier.TIER_1);
        assertThat(query.active()).isTrue();
    }

    @Test
    void acceptsNullFilters() {
        ListBusinessServicesQuery query = query(0, 20);

        assertThat(query.ownerTeamId()).isNull();
        assertThat(query.tier()).isNull();
        assertThat(query.active()).isNull();
    }

    @Test
    void rejectsNegativePage() {
        assertThatThrownBy(() -> query(-1, 20))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("page must be greater than or equal to 0");
    }

    @Test
    void rejectsNonPositiveSize() {
        assertThatThrownBy(() -> query(0, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("size must be greater than 0");
        assertThatThrownBy(() -> query(0, -1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("size must be greater than 0");
    }

    @Test
    void rejectsSizeAboveMaximum() {
        assertThatThrownBy(() -> query(0, 101))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("size must not exceed 100");
    }

    @Test
    void rejectsNonPositiveOwnerTeamId() {
        assertThatThrownBy(() -> new ListBusinessServicesQuery(0, 20, 0L, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("ownerTeamId must be positive");
        assertThatThrownBy(() -> new ListBusinessServicesQuery(0, 20, -1L, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("ownerTeamId must be positive");
    }

    private static ListBusinessServicesQuery query(int page, int size) {
        return new ListBusinessServicesQuery(page, size, null, null, null);
    }
}
