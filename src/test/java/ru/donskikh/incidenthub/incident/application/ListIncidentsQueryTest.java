package ru.donskikh.incidenthub.incident.application;

import org.junit.jupiter.api.Test;
import ru.donskikh.incidenthub.incident.IncidentPriority;
import ru.donskikh.incidenthub.incident.IncidentStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ListIncidentsQueryTest {

    @Test
    void acceptsValidPageAndSize() {
        ListIncidentsQuery query = new ListIncidentsQuery(2, 100, null, null, null);

        assertThat(query.page()).isEqualTo(2);
        assertThat(query.size()).isEqualTo(100);
    }

    @Test
    void rejectsNegativePage() {
        assertThatThrownBy(() -> new ListIncidentsQuery(-1, 20, null, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("page must be greater than or equal to 0");
    }

    @Test
    void rejectsZeroSize() {
        assertThatThrownBy(() -> new ListIncidentsQuery(0, 0, null, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("size must be greater than 0");
    }

    @Test
    void rejectsNegativeSize() {
        assertThatThrownBy(() -> new ListIncidentsQuery(0, -1, null, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("size must be greater than 0");
    }

    @Test
    void rejectsSizeAboveMaximum() {
        assertThatThrownBy(() -> new ListIncidentsQuery(0, 101, null, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("size must not exceed 100");
    }

    @Test
    void trimsCategory() {
        ListIncidentsQuery query = new ListIncidentsQuery(0, 20, null, null, "  hardware  ");

        assertThat(query.category()).isEqualTo("hardware");
    }

    @Test
    void convertsBlankCategoryToNull() {
        ListIncidentsQuery query = new ListIncidentsQuery(0, 20, null, null, "   ");

        assertThat(query.category()).isNull();
    }

    @Test
    void keepsNullCategory() {
        ListIncidentsQuery query = new ListIncidentsQuery(0, 20, null, null, null);

        assertThat(query.category()).isNull();
    }

    @Test
    void keepsStatusAndPriority() {
        ListIncidentsQuery query = new ListIncidentsQuery(
                0,
                20,
                IncidentStatus.IN_PROGRESS,
                IncidentPriority.CRITICAL,
                null
        );

        assertThat(query.status()).isEqualTo(IncidentStatus.IN_PROGRESS);
        assertThat(query.priority()).isEqualTo(IncidentPriority.CRITICAL);
    }
}
