package ru.donskikh.incidenthub.incident;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
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

    @Test
    void startsProgressForAssignedIncidentWithoutChangingClassificationOrAssignment() {
        User assignee = new User("assignee@example.com", "Assignee");
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
        incident.assignTo(assignee);

        incident.startProgress();

        assertThat(incident.getStatus()).isEqualTo(IncidentStatus.IN_PROGRESS);
        assertThat(incident.getAssignee()).isSameAs(assignee);
        assertThat(incident.getResponsibleTeam()).isSameAs(team);
        assertThat(incident.getCategory()).isEqualTo(IncidentCategory.APPLICATION);
        assertThat(incident.getSource()).isEqualTo(IncidentSource.AUTOMATIC);
    }

    @ParameterizedTest
    @EnumSource(value = IncidentStatus.class, names = "ASSIGNED", mode = EnumSource.Mode.EXCLUDE)
    void rejectsStartingProgressFromAnyStatusExceptAssigned(IncidentStatus status) throws Exception {
        Incident incident = createIncident();
        setStatus(incident, status);

        assertThatThrownBy(incident::startProgress)
                .isInstanceOf(IncidentStartProgressNotAllowedException.class)
                .hasMessage("Incident cannot start progress in status " + status)
                .extracting("status")
                .isEqualTo(status);
        assertThat(incident.getStatus()).isEqualTo(status);
    }

    @ParameterizedTest
    @CsvSource({
            "OPEN, false",
            "ASSIGNED, true",
            "IN_PROGRESS, false",
            "RESOLVED, false",
            "CLOSED, false",
            "CANCELLED, false"
    })
    void reportsWhetherStatusAllowsStartingProgress(IncidentStatus status, boolean expected) {
        assertThat(status.allowsStartProgress()).isEqualTo(expected);
    }

    @Test
    void resolvesInProgressIncidentWithoutChangingClassificationOrAssignment() {
        User reporter = new User("reporter@example.com", "Reporter");
        User assignee = new User("assignee@example.com", "Assignee");
        Team team = new Team("Platform", "platform");
        Incident incident = new Incident(
                "Title",
                "Description",
                IncidentCategory.APPLICATION,
                IncidentSource.AUTOMATIC,
                IncidentPriority.HIGH,
                reporter,
                team
        );
        incident.assignTo(assignee);
        incident.startProgress();

        incident.resolve();

        assertThat(incident.getStatus()).isEqualTo(IncidentStatus.RESOLVED);
        assertThat(incident.getAssignee()).isSameAs(assignee);
        assertThat(incident.getReporter()).isSameAs(reporter);
        assertThat(incident.getResponsibleTeam()).isSameAs(team);
        assertThat(incident.getCategory()).isEqualTo(IncidentCategory.APPLICATION);
        assertThat(incident.getSource()).isEqualTo(IncidentSource.AUTOMATIC);
    }

    @ParameterizedTest
    @EnumSource(value = IncidentStatus.class, names = "IN_PROGRESS", mode = EnumSource.Mode.EXCLUDE)
    void rejectsResolvingFromAnyStatusExceptInProgress(IncidentStatus status) throws Exception {
        Incident incident = createIncident();
        setStatus(incident, status);

        assertThatThrownBy(incident::resolve)
                .isInstanceOf(IncidentResolutionNotAllowedException.class)
                .hasMessage("Incident cannot be resolved in status " + status)
                .extracting("status")
                .isEqualTo(status);
        assertThat(incident.getStatus()).isEqualTo(status);
    }

    @ParameterizedTest
    @CsvSource({
            "OPEN, false",
            "ASSIGNED, false",
            "IN_PROGRESS, true",
            "RESOLVED, false",
            "CLOSED, false",
            "CANCELLED, false"
    })
    void reportsWhetherStatusAllowsResolving(IncidentStatus status, boolean expected) {
        assertThat(status.allowsResolve()).isEqualTo(expected);
    }

    @Test
    void closesResolvedIncidentWithoutChangingClassificationOrAssignment() {
        User reporter = new User("reporter@example.com", "Reporter");
        User assignee = new User("assignee@example.com", "Assignee");
        Team team = new Team("Platform", "platform");
        Incident incident = new Incident(
                "Title",
                "Description",
                IncidentCategory.APPLICATION,
                IncidentSource.AUTOMATIC,
                IncidentPriority.HIGH,
                reporter,
                team
        );
        incident.assignTo(assignee);
        incident.startProgress();
        incident.resolve();

        incident.close();

        assertThat(incident.getStatus()).isEqualTo(IncidentStatus.CLOSED);
        assertThat(incident.getAssignee()).isSameAs(assignee);
        assertThat(incident.getReporter()).isSameAs(reporter);
        assertThat(incident.getResponsibleTeam()).isSameAs(team);
        assertThat(incident.getCategory()).isEqualTo(IncidentCategory.APPLICATION);
        assertThat(incident.getSource()).isEqualTo(IncidentSource.AUTOMATIC);
    }

    @ParameterizedTest
    @EnumSource(value = IncidentStatus.class, names = "RESOLVED", mode = EnumSource.Mode.EXCLUDE)
    void rejectsClosingFromAnyStatusExceptResolved(IncidentStatus status) throws Exception {
        Incident incident = createIncident();
        setStatus(incident, status);

        assertThatThrownBy(incident::close)
                .isInstanceOf(IncidentClosureNotAllowedException.class)
                .hasMessage("Incident cannot be closed in status " + status)
                .extracting("status")
                .isEqualTo(status);
        assertThat(incident.getStatus()).isEqualTo(status);
    }

    @ParameterizedTest
    @CsvSource({
            "OPEN, false",
            "ASSIGNED, false",
            "IN_PROGRESS, false",
            "RESOLVED, true",
            "CLOSED, false",
            "CANCELLED, false"
    })
    void reportsWhetherStatusAllowsClosing(IncidentStatus status, boolean expected) {
        assertThat(status.allowsClose()).isEqualTo(expected);
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

    private static void setStatus(Incident incident, IncidentStatus status) throws Exception {
        var statusField = Incident.class.getDeclaredField("status");
        statusField.setAccessible(true);
        statusField.set(incident, status);
    }
}
