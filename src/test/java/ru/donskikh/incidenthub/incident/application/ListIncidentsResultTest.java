package ru.donskikh.incidenthub.incident.application;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ListIncidentsResultTest {

    @Test
    void rejectsNullItems() {
        assertThatThrownBy(() -> new ListIncidentsResult(null, 0, 20, 0, 0, false, false))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("items must not be null");
    }

    @Test
    void createsDefensiveCopyOfItems() {
        List<ListIncidentItem> source = new ArrayList<>();
        ListIncidentsResult result = new ListIncidentsResult(source, 0, 20, 0, 0, false, false);

        source.add(null);

        assertThat(result.items()).isEmpty();
        assertThatThrownBy(() -> result.items().add(null))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void returnsPageMetadata() {
        ListIncidentsResult result = new ListIncidentsResult(List.of(), 2, 15, 61, 5, true, true);

        assertThat(result.page()).isEqualTo(2);
        assertThat(result.size()).isEqualTo(15);
        assertThat(result.totalElements()).isEqualTo(61);
        assertThat(result.totalPages()).isEqualTo(5);
        assertThat(result.hasNext()).isTrue();
        assertThat(result.hasPrevious()).isTrue();
    }
}
