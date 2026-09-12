package ru.donskikh.incidenthub.team.application;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ListTeamsResultDefensiveCopyTest {

    @Test
    void defensivelyCopiesItems() {
        List<ListTeamItem> source = new ArrayList<>();
        ListTeamsResult result = new ListTeamsResult(source, 0, 20, 0, 0, false, false);

        source.add(null);

        assertThat(result.items()).isEmpty();
        assertThatThrownBy(() -> result.items().add(null))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void rejectsNullItems() {
        assertThatThrownBy(() -> new ListTeamsResult(null, 0, 20, 0, 0, false, false))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("items must not be null");
    }
}
