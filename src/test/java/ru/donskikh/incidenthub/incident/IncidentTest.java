package ru.donskikh.incidenthub.incident;

import org.junit.jupiter.api.Test;
import ru.donskikh.incidenthub.identity.User;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IncidentTest {

    @Test
    void createsIncidentWithOpenStatus() {
        Incident incident = createIncident();

        assertThat(incident.getStatus()).isEqualTo(IncidentStatus.OPEN);
    }

    @Test
    void keepsReporter() {
        User reporter = new User("reporter@example.com", "Reporter");

        Incident incident = new Incident(
                "Title",
                "Description",
                "Infrastructure",
                IncidentPriority.HIGH,
                reporter
        );

        assertThat(incident.getReporter()).isSameAs(reporter);
    }

    @Test
    void hasNoInitialAssignee() {
        Incident incident = createIncident();

        assertThat(incident.getAssignee()).isNull();
    }

    @Test
    void rejectsBlankRequiredStrings() {
        User reporter = new User("reporter@example.com", "Reporter");

        assertThatThrownBy(() -> new Incident("   ", "Description", "Infrastructure", IncidentPriority.HIGH, reporter))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("title must not be blank");
        assertThatThrownBy(() -> new Incident("Title", "   ", "Infrastructure", IncidentPriority.HIGH, reporter))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("description must not be blank");
        assertThatThrownBy(() -> new Incident("Title", "Description", "   ", IncidentPriority.HIGH, reporter))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("category must not be blank");
    }

    @Test
    void rejectsNullPriority() {
        User reporter = new User("reporter@example.com", "Reporter");

        assertThatThrownBy(() -> new Incident("Title", "Description", "Infrastructure", null, reporter))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("priority must not be null");
    }

    @Test
    void rejectsNullReporter() {
        assertThatThrownBy(() -> new Incident("Title", "Description", "Infrastructure", IncidentPriority.HIGH, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("reporter must not be null");
    }

    @Test
    void changesTitleDescriptionCategoryAndPriority() {
        Incident incident = createIncident();

        incident.changeTitle("Updated title");
        incident.updateDescription("Updated description");
        incident.changeCategory("Application");
        incident.changePriority(IncidentPriority.CRITICAL);

        assertThat(incident.getTitle()).isEqualTo("Updated title");
        assertThat(incident.getDescription()).isEqualTo("Updated description");
        assertThat(incident.getCategory()).isEqualTo("Application");
        assertThat(incident.getPriority()).isEqualTo(IncidentPriority.CRITICAL);
    }

    private static Incident createIncident() {
        return new Incident(
                "Title",
                "Description",
                "Infrastructure",
                IncidentPriority.HIGH,
                new User("reporter@example.com", "Reporter")
        );
    }
}
