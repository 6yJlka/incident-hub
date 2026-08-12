package ru.donskikh.incidenthub.incident;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
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

    @Test
    void assignsOpenIncident() {
        Incident incident = createIncident();
        User assignee = new User("assignee@example.com", "Assignee");

        incident.assignTo(assignee);

        assertThat(incident.getAssignee()).isSameAs(assignee);
        assertThat(incident.getStatus()).isEqualTo(IncidentStatus.ASSIGNED);
    }

    @Test
    void reassignsAssignedIncidentToAnotherUser() {
        Incident incident = createIncident();
        User firstAssignee = new User("first@example.com", "First Assignee");
        User secondAssignee = new User("second@example.com", "Second Assignee");
        incident.assignTo(firstAssignee);

        incident.assignTo(secondAssignee);

        assertThat(incident.getAssignee()).isSameAs(secondAssignee);
        assertThat(incident.getStatus()).isEqualTo(IncidentStatus.ASSIGNED);
    }

    @Test
    void allowsReassignmentToSameUser() {
        Incident incident = createIncident();
        User assignee = new User("assignee@example.com", "Assignee");
        incident.assignTo(assignee);

        incident.assignTo(assignee);

        assertThat(incident.getAssignee()).isSameAs(assignee);
        assertThat(incident.getStatus()).isEqualTo(IncidentStatus.ASSIGNED);
    }

    @Test
    void rejectsNullAssignee() {
        Incident incident = createIncident();

        assertThatThrownBy(() -> incident.assignTo(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("assignee must not be null");
    }

    @ParameterizedTest
    @CsvSource({
            "OPEN, true",
            "ASSIGNED, true",
            "IN_PROGRESS, false",
            "RESOLVED, false",
            "CLOSED, false",
            "CANCELLED, false"
    })
    void reportsWhetherStatusAllowsAssignment(IncidentStatus status, boolean expected) {
        assertThat(status.allowsAssignment()).isEqualTo(expected);
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
