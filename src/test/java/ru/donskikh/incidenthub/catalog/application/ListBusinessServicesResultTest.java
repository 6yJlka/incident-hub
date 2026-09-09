package ru.donskikh.incidenthub.catalog.application;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ListBusinessServicesResultTest {

    @Test
    void copiesItemList() {
        ArrayList<ListBusinessServiceItem> items = new ArrayList<>();
        ListBusinessServicesResult result = new ListBusinessServicesResult(
                items, 0, 20, 0, 0, false, false
        );

        items.add(null);

        assertThat(result.items()).isEmpty();
        assertThatThrownBy(() -> result.items().add(null))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void rejectsNullItemList() {
        assertThatThrownBy(() -> new ListBusinessServicesResult(
                null, 0, 20, 0, 0, false, false
        ))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("items must not be null");
    }
}
