package ru.donskikh.incidenthub.incident;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import ru.donskikh.incidenthub.catalog.BusinessService;
import ru.donskikh.incidenthub.catalog.ServiceTier;
import ru.donskikh.incidenthub.identity.User;
import ru.donskikh.incidenthub.team.Team;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IncidentTest {

    @Test
    void createsIncidentWithOpenStatusServiceAndSeverity() {
        Incident incident = createIncident();

        assertThat(incident.getStatus()).isEqualTo(IncidentStatus.OPEN);
        assertThat(incident.getAffectedService().getCode()).isEqualTo("PAYMENTS");
        assertThat(incident.getSource()).isEqualTo(IncidentSource.MANUAL);
        assertThat(incident.getSeverity()).isEqualTo(IncidentSeverity.SEV2);
    }

    @Test
    void keepsReporter() {
        User reporter = new User("reporter@example.com", "Reporter");

        Incident incident = new Incident(
                "Title",
                "Description",
                createBusinessService(),
                IncidentSource.MANUAL,
                IncidentPriority.HIGH,
                IncidentSeverity.SEV2,
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
                createBusinessService(),
                IncidentSource.AUTOMATIC,
                IncidentPriority.HIGH,
                IncidentSeverity.SEV2,
                new User("reporter@example.com", "Reporter"),
                team
        );

        assertThat(incident.getResponsibleTeam()).isSameAs(team);
    }

    @Test
    void rejectsBlankRequiredStrings() {
        User reporter = new User("reporter@example.com", "Reporter");

        assertThatThrownBy(() -> new Incident(
                "   ", "Description", createBusinessService(), IncidentSource.MANUAL,
                IncidentPriority.HIGH, IncidentSeverity.SEV2, reporter, null
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("title must not be blank");
        assertThatThrownBy(() -> new Incident(
                "Title", "   ", createBusinessService(), IncidentSource.MANUAL,
                IncidentPriority.HIGH, IncidentSeverity.SEV2, reporter, null
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("description must not be blank");
    }

    @Test
    void rejectsNullAffectedService() {
        User reporter = new User("reporter@example.com", "Reporter");

        assertThatThrownBy(() -> new Incident(
                "Title", "Description", null, IncidentSource.MANUAL,
                IncidentPriority.HIGH, IncidentSeverity.SEV2, reporter, null
        )).isInstanceOf(NullPointerException.class)
                .hasMessage("affectedService must not be null");
    }

    @Test
    void rejectsNullSource() {
        User reporter = new User("reporter@example.com", "Reporter");

        assertThatThrownBy(() -> new Incident(
                "Title", "Description", createBusinessService(), null,
                IncidentPriority.HIGH, IncidentSeverity.SEV2, reporter, null
        )).isInstanceOf(NullPointerException.class)
                .hasMessage("source must not be null");
    }

    @Test
    void rejectsNullPriority() {
        User reporter = new User("reporter@example.com", "Reporter");

        assertThatThrownBy(() -> new Incident(
                "Title", "Description", createBusinessService(), IncidentSource.MANUAL,
                null, IncidentSeverity.SEV2, reporter, null
        )).isInstanceOf(NullPointerException.class)
                .hasMessage("priority must not be null");
    }

    @Test
    void rejectsNullSeverity() {
        User reporter = new User("reporter@example.com", "Reporter");

        assertThatThrownBy(() -> new Incident(
                "Title", "Description", createBusinessService(), IncidentSource.MANUAL,
                IncidentPriority.HIGH, null, reporter, null
        )).isInstanceOf(NullPointerException.class)
                .hasMessage("severity must not be null");
    }

    @Test
    void rejectsNullReporter() {
        assertThatThrownBy(() -> new Incident(
                "Title", "Description", createBusinessService(), IncidentSource.MANUAL,
                IncidentPriority.HIGH, IncidentSeverity.SEV2, null, null
        )).isInstanceOf(NullPointerException.class)
                .hasMessage("reporter must not be null");
    }

    @Test
    void changesTitleDescriptionAffectedServicePriorityAndSeverity() {
        Incident incident = createIncident();
        BusinessService affectedService = new BusinessService(
                "CHECKOUT",
                "Checkout",
                "Checkout service",
                new Team("Payments", "PAYMENTS_TEAM"),
                ServiceTier.TIER_1
        );

        incident.changeTitle("Updated title");
        incident.updateDescription("Updated description");
        incident.changeAffectedService(affectedService);
        incident.changePriority(IncidentPriority.CRITICAL);
        incident.changeSeverity(IncidentSeverity.SEV1);

        assertThat(incident.getTitle()).isEqualTo("Updated title");
        assertThat(incident.getDescription()).isEqualTo("Updated description");
        assertThat(incident.getAffectedService()).isSameAs(affectedService);
        assertThat(incident.getPriority()).isEqualTo(IncidentPriority.CRITICAL);
        assertThat(incident.getSeverity()).isEqualTo(IncidentSeverity.SEV1);
    }

    @Test
    void rejectsNullAffectedServiceChange() {
        Incident incident = createIncident();

        assertThatThrownBy(() -> incident.changeAffectedService(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("affectedService must not be null");
    }

    @Test
    void rejectsNullSeverityChange() {
        Incident incident = createIncident();

        assertThatThrownBy(() -> incident.changeSeverity(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("severity must not be null");
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
    void startsProgressForAssignedIncidentWithoutChangingDetailsOrAssignment() {
        User assignee = new User("assignee@example.com", "Assignee");
        Team team = new Team("Platform", "platform");
        BusinessService affectedService = createBusinessService();
        Incident incident = new Incident(
                "Title",
                "Description",
                affectedService,
                IncidentSource.AUTOMATIC,
                IncidentPriority.HIGH,
                IncidentSeverity.SEV2,
                new User("reporter@example.com", "Reporter"),
                team
        );
        incident.assignTo(assignee);

        incident.startProgress();

        assertThat(incident.getStatus()).isEqualTo(IncidentStatus.IN_PROGRESS);
        assertThat(incident.getAssignee()).isSameAs(assignee);
        assertThat(incident.getResponsibleTeam()).isSameAs(team);
        assertThat(incident.getAffectedService()).isSameAs(affectedService);
        assertThat(incident.getSeverity()).isEqualTo(IncidentSeverity.SEV2);
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
    void resolvesInProgressIncidentWithoutChangingDetailsOrAssignment() {
        User reporter = new User("reporter@example.com", "Reporter");
        User assignee = new User("assignee@example.com", "Assignee");
        Team team = new Team("Platform", "platform");
        BusinessService affectedService = createBusinessService();
        Incident incident = new Incident(
                "Title",
                "Description",
                affectedService,
                IncidentSource.AUTOMATIC,
                IncidentPriority.HIGH,
                IncidentSeverity.SEV2,
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
        assertThat(incident.getAffectedService()).isSameAs(affectedService);
        assertThat(incident.getSeverity()).isEqualTo(IncidentSeverity.SEV2);
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
    void closesResolvedIncidentWithoutChangingDetailsOrAssignment() {
        User reporter = new User("reporter@example.com", "Reporter");
        User assignee = new User("assignee@example.com", "Assignee");
        Team team = new Team("Platform", "platform");
        BusinessService affectedService = createBusinessService();
        Incident incident = new Incident(
                "Title",
                "Description",
                affectedService,
                IncidentSource.AUTOMATIC,
                IncidentPriority.HIGH,
                IncidentSeverity.SEV2,
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
        assertThat(incident.getAffectedService()).isSameAs(affectedService);
        assertThat(incident.getSeverity()).isEqualTo(IncidentSeverity.SEV2);
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

    @Test
    void reopensResolvedIncidentWithoutChangingDetailsOrAssignment() {
        User reporter = new User("reporter@example.com", "Reporter");
        User assignee = new User("assignee@example.com", "Assignee");
        Team team = new Team("Platform", "platform");
        BusinessService affectedService = createBusinessService();
        Incident incident = new Incident(
                "Title",
                "Description",
                affectedService,
                IncidentSource.AUTOMATIC,
                IncidentPriority.HIGH,
                IncidentSeverity.SEV2,
                reporter,
                team
        );
        incident.assignTo(assignee);
        incident.startProgress();
        incident.resolve();

        incident.reopen();

        assertThat(incident.getStatus()).isEqualTo(IncidentStatus.IN_PROGRESS);
        assertThat(incident.getAssignee()).isSameAs(assignee);
        assertThat(incident.getReporter()).isSameAs(reporter);
        assertThat(incident.getResponsibleTeam()).isSameAs(team);
        assertThat(incident.getAffectedService()).isSameAs(affectedService);
        assertThat(incident.getSeverity()).isEqualTo(IncidentSeverity.SEV2);
        assertThat(incident.getSource()).isEqualTo(IncidentSource.AUTOMATIC);
    }

    @ParameterizedTest
    @EnumSource(value = IncidentStatus.class, names = {"OPEN", "ASSIGNED", "IN_PROGRESS", "CANCELLED"})
    void rejectsReopeningFromNonResolvedStatus(IncidentStatus status) throws Exception {
        Incident incident = createIncident();
        setStatus(incident, status);

        assertReopenNotAllowed(incident, status);
    }

    @Test
    void rejectsReopeningClosedIncidentReachedThroughLifecycle() {
        Incident incident = createIncident();
        incident.assignTo(new User("assignee@example.com", "Assignee"));
        incident.startProgress();
        incident.resolve();
        incident.close();

        assertReopenNotAllowed(incident, IncidentStatus.CLOSED);
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
    void reportsWhetherStatusAllowsReopening(IncidentStatus status, boolean expected) {
        assertThat(status.allowsReopen()).isEqualTo(expected);
    }

    @Test
    void cancelsOpenIncident() {
        Incident incident = createIncident();

        incident.cancel();

        assertThat(incident.getStatus()).isEqualTo(IncidentStatus.CANCELLED);
    }

    @Test
    void cancelsAssignedIncidentWithoutChangingDetailsOrAssignment() {
        User reporter = new User("reporter@example.com", "Reporter");
        User assignee = new User("assignee@example.com", "Assignee");
        Team team = new Team("Platform", "platform");
        BusinessService affectedService = createBusinessService();
        Incident incident = new Incident(
                "Title",
                "Description",
                affectedService,
                IncidentSource.AUTOMATIC,
                IncidentPriority.HIGH,
                IncidentSeverity.SEV2,
                reporter,
                team
        );
        incident.assignTo(assignee);

        incident.cancel();

        assertThat(incident.getStatus()).isEqualTo(IncidentStatus.CANCELLED);
        assertThat(incident.getAssignee()).isSameAs(assignee);
        assertThat(incident.getReporter()).isSameAs(reporter);
        assertThat(incident.getResponsibleTeam()).isSameAs(team);
        assertThat(incident.getAffectedService()).isSameAs(affectedService);
        assertThat(incident.getSeverity()).isEqualTo(IncidentSeverity.SEV2);
        assertThat(incident.getSource()).isEqualTo(IncidentSource.AUTOMATIC);
    }

    @Test
    void cancelsInProgressIncident() {
        Incident incident = createIncident();
        incident.assignTo(new User("assignee@example.com", "Assignee"));
        incident.startProgress();

        incident.cancel();

        assertThat(incident.getStatus()).isEqualTo(IncidentStatus.CANCELLED);
    }

    @Test
    void rejectsCancellingResolvedIncident() {
        Incident incident = createIncident();
        incident.assignTo(new User("assignee@example.com", "Assignee"));
        incident.startProgress();
        incident.resolve();

        assertCancellationNotAllowed(incident, IncidentStatus.RESOLVED);
    }

    @Test
    void rejectsCancellingClosedIncident() {
        Incident incident = createIncident();
        incident.assignTo(new User("assignee@example.com", "Assignee"));
        incident.startProgress();
        incident.resolve();
        incident.close();

        assertCancellationNotAllowed(incident, IncidentStatus.CLOSED);
    }

    @Test
    void rejectsCancellingAlreadyCancelledIncident() {
        Incident incident = createIncident();
        incident.cancel();

        assertCancellationNotAllowed(incident, IncidentStatus.CANCELLED);
    }

    @ParameterizedTest
    @CsvSource({
            "OPEN, true",
            "ASSIGNED, true",
            "IN_PROGRESS, true",
            "RESOLVED, false",
            "CLOSED, false",
            "CANCELLED, false"
    })
    void reportsWhetherStatusAllowsCancellation(IncidentStatus status, boolean expected) {
        assertThat(status.allowsCancellation()).isEqualTo(expected);
    }

    private static Incident createIncident() {
        return new Incident(
                "Title",
                "Description",
                createBusinessService(),
                IncidentSource.MANUAL,
                IncidentPriority.HIGH,
                IncidentSeverity.SEV2,
                new User("reporter@example.com", "Reporter"),
                null
        );
    }

    private static BusinessService createBusinessService() {
        return new BusinessService(
                "PAYMENTS",
                "Payments",
                "Payments service",
                new Team("Payments", "PAYMENTS_TEAM"),
                ServiceTier.TIER_1
        );
    }

    private static void setStatus(Incident incident, IncidentStatus status) throws Exception {
        var statusField = Incident.class.getDeclaredField("status");
        statusField.setAccessible(true);
        statusField.set(incident, status);
    }

    private static void assertReopenNotAllowed(Incident incident, IncidentStatus status) {
        assertThatThrownBy(incident::reopen)
                .isInstanceOf(IncidentReopenNotAllowedException.class)
                .hasMessage("Incident cannot be reopened in status " + status)
                .extracting("status")
                .isEqualTo(status);
        assertThat(incident.getStatus()).isEqualTo(status);
    }

    private static void assertCancellationNotAllowed(Incident incident, IncidentStatus status) {
        assertThatThrownBy(incident::cancel)
                .isInstanceOf(IncidentCancellationNotAllowedException.class)
                .hasMessage("Incident cannot be cancelled in status " + status)
                .extracting("status")
                .isEqualTo(status);
        assertThat(incident.getStatus()).isEqualTo(status);
    }
}
