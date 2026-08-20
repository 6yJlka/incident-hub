package ru.donskikh.incidenthub.audit.application;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GetIncidentHistoryResultTest {

    @Test
    void rejectsNullItems() {
        assertThatThrownBy(() -> new GetIncidentHistoryResult(42L, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("items must not be null");
    }

    @Test
    void createsDefensiveImmutableCopyOfItems() {
        List<IncidentHistoryItem> source = new ArrayList<>();
        GetIncidentHistoryResult result = new GetIncidentHistoryResult(42L, source);

        source.add(null);

        assertThat(result.items()).isEmpty();
        assertThatThrownBy(() -> result.items().add(null))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
