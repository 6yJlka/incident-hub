package ru.donskikh.incidenthub.incident;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import ru.donskikh.incidenthub.identity.User;
import ru.donskikh.incidenthub.team.Team;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IncidentTest {

    @Test
    void createsIncidentWithOpenStatusAndClassification() {
        Incident incident = createIncident();

        assertThat(incident.getStatus()).isEqualTo(IncidentStatus.OPEN);
        assertThat(incident.getCategory()).isEqualTo(IncidentCategory.INFRASTRUCTURE);
        assertThat(incident.getSource()).isEqualTo(IncidentSource.MANUAL);
    }

    @Test
    void keepsReporter() {
        User reporter = new User("reporter@example.com", "Reporter");

        Incident incident = new Incident(
                "Title",
                "Description",
                IncidentCategory.INFRASTRUCTURE,
                IncidentSource.MANUAL,
                IncidentPriority.HIGH,
                reporter,
                null
        );

        assertThat(incident.getReporter()).isSameAs(reporter);
    }

    @Test
    void hasNoInitialAssignee() {
        assertThat(createIncident().getAssignee()).isNull();
    }

    @Test
    void allowsNullResponsibleTeam() {
        assertThat(createIncident().getResponsibleTeam()).isNull();
    }

    @Test
    void keepsResponsibleTeam() {
        Team team = new Team("Platform", "platform");

        Incident incident = new Incident(
                "Title",
                "Description",
                IncidentCategory.APPLICATION,
                IncidentSource.AUTOMATIC,
                IncidentPriority.HIGH,
                new User("reporter@example.com", "Reporter"),
                team
        );

        assertThat(incident.getResponsibleTeam()).isSameAs(team);
    }

    @Test
    void rejectsBlankRequiredStrings() {
        User reporter = new User("reporter@example.com", "Reporter");

        assertThatThrownBy(() -> new Incident(
                "   ", "Description", IncidentCategory.INFRASTRUCTURE, IncidentSource.MANUAL,
                IncidentPriority.HIGH, reporter, null
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("title must not be blank");
        assertThatThrownBy(() -> new Incident(
                "Title", "   ", IncidentCategory.INFRASTRUCTURE, IncidentSource.MANUAL,
                IncidentPriority.HIGH, reporter, null
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("description must not be blank");
    }

    @Test
    void rejectsNullCategory() {
        User reporter = new User("reporter@example.com", "Reporter");

        assertThatThrownBy(() -> new Incident(
                "Title", "Description", null, IncidentSource.MANUAL,
                IncidentPriority.HIGH, reporter, null
        )).isInstanceOf(NullPointerException.class)
                .hasMessage("category must not be null");
    }

    @Test
    void rejectsNullSource() {
        User reporter = new User("reporter@example.com", "Reporter");

        assertThatThrownBy(() -> new Incident(
                "Title", "Description", IncidentCategory.INFRASTRUCTURE, null,
                IncidentPriority.HIGH, reporter, null
        )).isInstanceOf(NullPointerException.class)
                .hasMessage("source must not be null");
    }

    @Test
    void rejectsNullPriority() {
        User reporter = new User("reporter@example.com", "Reporter");

        assertThatThrownBy(() -> new Incident(
                "Title", "Description", IncidentCategory.INFRASTRUCTURE, IncidentSource.MANUAL,
                null, reporter, null
        )).isInstanceOf(NullPointerException.class)
                .hasMessage("priority must not be null");
    }

    @Test
    void rejectsNullReporter() {
        assertThatThrownBy(() -> new Incident(
                "Title", "Description", IncidentCategory.INFRASTRUCTURE, IncidentSource.MANUAL,
                IncidentPriority.HIGH, null, null
        )).isInstanceOf(NullPointerException.class)
                .hasMessage("reporter must not be null");
    }

    @Test
    void changesTitleDescriptionCategoryAndPriority() {
        Incident incident = createIncident();

        incident.changeTitle("Updated title");
        incident.updateDescription("Updated description");
        incident.changeCategory(IncidentCategory.APPLICATION);
        incident.changePriority(IncidentPriority.CRITICAL);

        assertThat(incident.getTitle()).isEqualTo("Updated title");
        assertThat(incident.getDescription()).isEqualTo("Updated description");
        assertThat(incident.getCategory()).isEqualTo(IncidentCategory.APPLICATION);
        assertThat(incident.getPriority()).isEqualTo(IncidentPriority.CRITICAL);
    }

    @Test
    void rejectsNullCategoryChange() {
        Incident incident = createIncident();

        assertThatThrownBy(() -> incident.changeCategory(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("category must not be null");
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
                IncidentCategory.INFRASTRUCTURE,
                IncidentSource.MANUAL,
                IncidentPriority.HIGH,
                new User("reporter@example.com", "Reporter"),
                null
        );
    }
}
