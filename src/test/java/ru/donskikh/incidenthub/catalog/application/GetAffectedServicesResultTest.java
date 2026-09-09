package ru.donskikh.incidenthub.catalog.application;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GetAffectedServicesResultTest {

    @Test
    void copiesItemList() {
        ArrayList<AffectedServiceItem> items = new ArrayList<>();
        GetAffectedServicesResult result = new GetAffectedServicesResult(items, 42L, 3);

        items.add(null);

        assertThat(result.items()).isEmpty();
        assertThatThrownBy(() -> result.items().add(null))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void rejectsNullItemList() {
        assertThatThrownBy(() -> new GetAffectedServicesResult(null, 42L, 3))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("items must not be null");
    }
}
