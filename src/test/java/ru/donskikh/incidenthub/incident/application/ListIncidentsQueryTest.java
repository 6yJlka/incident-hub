package ru.donskikh.incidenthub.incident.application;

import org.junit.jupiter.api.Test;
import ru.donskikh.incidenthub.incident.IncidentPriority;
import ru.donskikh.incidenthub.incident.IncidentSeverity;
import ru.donskikh.incidenthub.incident.IncidentSource;
import ru.donskikh.incidenthub.incident.IncidentStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ListIncidentsQueryTest {

    @Test
    void acceptsValidPageSizeAndFilters() {
        ListIncidentsQuery query = new ListIncidentsQuery(
                2, 100, IncidentStatus.OPEN, IncidentPriority.HIGH,
                IncidentSeverity.SEV2, IncidentSource.AUTOMATIC, 11L, 9L
        );

        assertThat(query.page()).isEqualTo(2);
        assertThat(query.size()).isEqualTo(100);
        assertThat(query.status()).isEqualTo(IncidentStatus.OPEN);
        assertThat(query.priority()).isEqualTo(IncidentPriority.HIGH);
        assertThat(query.severity()).isEqualTo(IncidentSeverity.SEV2);
        assertThat(query.source()).isEqualTo(IncidentSource.AUTOMATIC);
        assertThat(query.affectedServiceId()).isEqualTo(11L);
        assertThat(query.responsibleTeamId()).isEqualTo(9L);
    }

    @Test
    void acceptsNullFilters() {
        ListIncidentsQuery query = query(0, 20);

        assertThat(query.status()).isNull();
        assertThat(query.priority()).isNull();
        assertThat(query.severity()).isNull();
        assertThat(query.source()).isNull();
        assertThat(query.affectedServiceId()).isNull();
        assertThat(query.responsibleTeamId()).isNull();
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
    void rejectsNonPositiveAffectedServiceId() {
        assertThatThrownBy(() -> new ListIncidentsQuery(
                0, 20, null, null, null, null, 0L, null
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("affectedServiceId must be positive");
        assertThatThrownBy(() -> new ListIncidentsQuery(
                0, 20, null, null, null, null, -1L, null
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("affectedServiceId must be positive");
    }

    @Test
    void rejectsNonPositiveResponsibleTeamId() {
        assertThatThrownBy(() -> new ListIncidentsQuery(
                0, 20, null, null, null, null, null, 0L
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("responsibleTeamId must be positive");
        assertThatThrownBy(() -> new ListIncidentsQuery(
                0, 20, null, null, null, null, null, -1L
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("responsibleTeamId must be positive");
    }

    private static ListIncidentsQuery query(int page, int size) {
        return new ListIncidentsQuery(page, size, null, null, null, null, null, null);
    }
}
