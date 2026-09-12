package ru.donskikh.incidenthub.incident.web;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import ru.donskikh.incidenthub.audit.IncidentAuditEventType;
import ru.donskikh.incidenthub.audit.application.GetIncidentHistoryResult;
import ru.donskikh.incidenthub.audit.application.GetIncidentHistoryService;
import ru.donskikh.incidenthub.audit.application.IncidentHistoryItem;
import ru.donskikh.incidenthub.catalog.BusinessServiceNotFoundException;
import ru.donskikh.incidenthub.common.web.GlobalExceptionHandler;
import ru.donskikh.incidenthub.incident.IncidentAssignmentNotAllowedException;
import ru.donskikh.incidenthub.incident.IncidentClosureNotAllowedException;
import ru.donskikh.incidenthub.incident.IncidentNotFoundException;
import ru.donskikh.incidenthub.incident.IncidentPriority;
import ru.donskikh.incidenthub.incident.IncidentSeverity;
import ru.donskikh.incidenthub.incident.IncidentSource;
import ru.donskikh.incidenthub.incident.IncidentStatus;
import ru.donskikh.incidenthub.incident.application.AssignIncidentCommand;
import ru.donskikh.incidenthub.incident.application.AssignIncidentResult;
import ru.donskikh.incidenthub.incident.application.AssignIncidentService;
import ru.donskikh.incidenthub.incident.application.CancelIncidentCommand;
import ru.donskikh.incidenthub.incident.application.CancelIncidentResult;
import ru.donskikh.incidenthub.incident.application.CancelIncidentService;
import ru.donskikh.incidenthub.incident.application.CloseIncidentCommand;
import ru.donskikh.incidenthub.incident.application.CloseIncidentResult;
import ru.donskikh.incidenthub.incident.application.CloseIncidentService;
import ru.donskikh.incidenthub.incident.application.CreateIncidentCommand;
import ru.donskikh.incidenthub.incident.application.CreateIncidentResult;
import ru.donskikh.incidenthub.incident.application.CreateIncidentService;
import ru.donskikh.incidenthub.incident.application.GetIncidentResult;
import ru.donskikh.incidenthub.incident.application.GetIncidentService;
import ru.donskikh.incidenthub.incident.application.ListIncidentItem;
import ru.donskikh.incidenthub.incident.application.ListIncidentsQuery;
import ru.donskikh.incidenthub.incident.application.ListIncidentsResult;
import ru.donskikh.incidenthub.incident.application.ListIncidentsService;
import ru.donskikh.incidenthub.incident.application.ReopenIncidentCommand;
import ru.donskikh.incidenthub.incident.application.ReopenIncidentResult;
import ru.donskikh.incidenthub.incident.application.ReopenIncidentService;
import ru.donskikh.incidenthub.incident.application.ResolveIncidentCommand;
import ru.donskikh.incidenthub.incident.application.ResolveIncidentResult;
import ru.donskikh.incidenthub.incident.application.ResolveIncidentService;
import ru.donskikh.incidenthub.incident.application.StartIncidentProgressCommand;
import ru.donskikh.incidenthub.incident.application.StartIncidentProgressResult;
import ru.donskikh.incidenthub.incident.application.StartIncidentProgressService;

import java.time.Instant;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.hasKey;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(IncidentController.class)
@Import({IncidentWebMapper.class, GlobalExceptionHandler.class})
class IncidentControllerTest {

    private static final String VALID_CREATE_REQUEST = """
            {
              "title": "Недоступна оплата",
              "description": "Платежи завершаются ошибкой",
              "affectedServiceId": 10,
              "priority": "HIGH",
              "severity": "SEV2",
              "reporterId": 20
            }
            """;

    private static final Instant CREATED_AT = Instant.parse("2026-09-11T08:00:00Z");
    private static final Instant UPDATED_AT = Instant.parse("2026-09-11T08:05:00Z");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreateIncidentService createIncidentService;

    @MockitoBean
    private ListIncidentsService listIncidentsService;

    @MockitoBean
    private GetIncidentService getIncidentService;

    @MockitoBean
    private GetIncidentHistoryService getIncidentHistoryService;

    @MockitoBean
    private AssignIncidentService assignIncidentService;

    @MockitoBean
    private StartIncidentProgressService startIncidentProgressService;

    @MockitoBean
    private ResolveIncidentService resolveIncidentService;

    @MockitoBean
    private CloseIncidentService closeIncidentService;

    @MockitoBean
    private ReopenIncidentService reopenIncidentService;

    @MockitoBean
    private CancelIncidentService cancelIncidentService;

    @Test
    void createsIncident() throws Exception {
        when(createIncidentService.create(any(CreateIncidentCommand.class)))
                .thenReturn(new CreateIncidentResult(42L, IncidentStatus.OPEN));

        mockMvc.perform(post("/api/v1/incidents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_CREATE_REQUEST))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/v1/incidents/42"))
                .andExpect(jsonPath("$.incidentId").value(42))
                .andExpect(jsonPath("$.status").value("OPEN"));

        verify(createIncidentService).create(new CreateIncidentCommand(
                "Недоступна оплата",
                "Платежи завершаются ошибкой",
                10L,
                IncidentPriority.HIGH,
                IncidentSeverity.SEV2,
                20L,
                null
        ));
    }

    @Test
    void listsIncidentsWithFiltersAndPagination() throws Exception {
        ListIncidentItem item = listItem(IncidentStatus.IN_PROGRESS);
        when(listIncidentsService.execute(any(ListIncidentsQuery.class)))
                .thenReturn(new ListIncidentsResult(List.of(item), 1, 5, 8, 2, false, true));

        mockMvc.perform(get("/api/v1/incidents")
                        .param("page", "1")
                        .param("size", "5")
                        .param("status", "IN_PROGRESS")
                        .param("priority", "HIGH")
                        .param("severity", "SEV2")
                        .param("source", "MANUAL")
                        .param("affectedServiceId", "10")
                        .param("responsibleTeamId", "30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(42))
                .andExpect(jsonPath("$.items[0].title").value("Недоступна оплата"))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(5))
                .andExpect(jsonPath("$.totalElements").value(8))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.hasNext").value(false))
                .andExpect(jsonPath("$.hasPrevious").value(true))
                .andExpect(jsonPath("$", not(hasKey("content"))));

        verify(listIncidentsService).execute(new ListIncidentsQuery(
                1,
                5,
                IncidentStatus.IN_PROGRESS,
                IncidentPriority.HIGH,
                IncidentSeverity.SEV2,
                IncidentSource.MANUAL,
                10L,
                30L
        ));
    }

    @Test
    void getsIncident() throws Exception {
        when(getIncidentService.get(42L)).thenReturn(incident(IncidentStatus.OPEN, null));

        mockMvc.perform(get("/api/v1/incidents/42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(42))
                .andExpect(jsonPath("$.title").value("Недоступна оплата"))
                .andExpect(jsonPath("$.affectedServiceCode").value("PAYMENTS"))
                .andExpect(jsonPath("$.status").value("OPEN"));
    }

    @Test
    void getsIncidentHistory() throws Exception {
        when(getIncidentHistoryService.get(42L)).thenReturn(new GetIncidentHistoryResult(
                42L,
                List.of(new IncidentHistoryItem(
                        100L,
                        IncidentAuditEventType.ASSIGNED,
                        IncidentStatus.OPEN,
                        IncidentStatus.ASSIGNED,
                        UPDATED_AT
                ))
        ));

        mockMvc.perform(get("/api/v1/incidents/42/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.incidentId").value(42))
                .andExpect(jsonPath("$.items[0].id").value(100))
                .andExpect(jsonPath("$.items[0].eventType").value("ASSIGNED"))
                .andExpect(jsonPath("$.items[0].fromStatus").value("OPEN"))
                .andExpect(jsonPath("$.items[0].toStatus").value("ASSIGNED"));
    }

    @Test
    void assignsIncidentAndReturnsUpdatedCard() throws Exception {
        when(assignIncidentService.assign(any(AssignIncidentCommand.class)))
                .thenReturn(new AssignIncidentResult(42L, 21L, IncidentStatus.ASSIGNED));
        when(getIncidentService.get(42L)).thenReturn(incident(IncidentStatus.ASSIGNED, 21L));

        mockMvc.perform(post("/api/v1/incidents/42/assign")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"assigneeId\":21}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(42))
                .andExpect(jsonPath("$.assigneeId").value(21))
                .andExpect(jsonPath("$.status").value("ASSIGNED"));

        verify(assignIncidentService).assign(new AssignIncidentCommand(42L, 21L));
    }

    @ParameterizedTest
    @MethodSource("successfulLifecycleActions")
    void performsLifecycleActionAndReturnsUpdatedCard(
            String path,
            LifecycleAction action,
            IncidentStatus expectedStatus
    ) throws Exception {
        stubSuccessfulLifecycleAction(action, expectedStatus);
        when(getIncidentService.get(42L)).thenReturn(incident(expectedStatus, 21L));

        mockMvc.perform(post(path))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(42))
                .andExpect(jsonPath("$.status").value(expectedStatus.name()));
    }

    @ParameterizedTest
    @MethodSource("notFoundRequests")
    void mapsNotFoundForEveryEndpoint(
            MockHttpServletRequestBuilder request,
            Endpoint endpoint
    ) throws Exception {
        stubNotFound(endpoint);
        String expectedDetail = endpoint == Endpoint.CREATE
                ? "Business service not found: 42"
                : "Incident not found: 42";

        mockMvc.perform(request)
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:incident-hub:problem:not-found"))
                .andExpect(jsonPath("$.title").value("Resource not found"))
                .andExpect(jsonPath("$.detail").value(expectedDetail))
                .andExpect(jsonPath("$.instance").value(request.buildRequest(null).getRequestURI()));
    }

    @ParameterizedTest(name = "missing {0}")
    @MethodSource("createRequestsWithMissingRequiredField")
    void rejectsEachMissingRequiredCreateField(String field, String requestBody) throws Exception {
        mockMvc.perform(post("/api/v1/incidents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:incident-hub:problem:validation"))
                .andExpect(jsonPath("$.errors[*].field", hasItem(field)));
    }

    @Test
    void rejectsInvalidAssignBody() throws Exception {
        mockMvc.perform(post("/api/v1/incidents/42/assign")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.errors[*].field", hasItem("assigneeId")));
    }

    @Test
    void rejectsInvalidListQuery() throws Exception {
        mockMvc.perform(get("/api/v1/incidents").param("size", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("size must be greater than 0"));
    }

    @Test
    void rejectsUnknownQueryParameterValue() throws Exception {
        mockMvc.perform(get("/api/v1/incidents").param("status", "UNKNOWN"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:incident-hub:problem:invalid-parameter"))
                .andExpect(jsonPath("$.detail").value("Parameter 'status' has an invalid value"));
    }

    @ParameterizedTest
    @MethodSource("readRequestsWithInvalidId")
    void rejectsInvalidIdForReadEndpoints(
            MockHttpServletRequestBuilder request,
            ReadEndpoint endpoint
    ) throws Exception {
        if (endpoint == ReadEndpoint.GET) {
            when(getIncidentService.get(0L)).thenThrow(new IllegalArgumentException("incidentId must be positive"));
        } else {
            when(getIncidentHistoryService.get(0L))
                    .thenThrow(new IllegalArgumentException("incidentId must be positive"));
        }

        mockMvc.perform(request)
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("incidentId must be positive"));
    }

    @ParameterizedTest
    @MethodSource("lifecycleRequestsWithInvalidId")
    void rejectsInvalidIdForLifecycleEndpoints(MockHttpServletRequestBuilder request) throws Exception {
        mockMvc.perform(request)
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("incidentId must be positive"));
    }

    @Test
    void rejectsTruncatedJson() throws Exception {
        mockMvc.perform(post("/api/v1/incidents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\": \"Недоступна оплата\""))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:incident-hub:problem:malformed-request"))
                .andExpect(jsonPath("$.title").value("Malformed request body"))
                .andExpect(jsonPath("$.detail").value("Request body could not be read"));
    }

    @Test
    void mapsForbiddenLifecycleTransitionToConflictWithStatuses() throws Exception {
        when(closeIncidentService.close(any(CloseIncidentCommand.class)))
                .thenThrow(new IncidentClosureNotAllowedException(42L, IncidentStatus.IN_PROGRESS));

        mockMvc.perform(post("/api/v1/incidents/42/close"))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:incident-hub:problem:conflict"))
                .andExpect(jsonPath("$.currentStatus").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.requiredStatus").value("RESOLVED"));
    }

    @Test
    void mapsMultipleRequiredStatusesAsArray() throws Exception {
        when(assignIncidentService.assign(any(AssignIncidentCommand.class)))
                .thenThrow(new IncidentAssignmentNotAllowedException(42L, IncidentStatus.CLOSED));

        mockMvc.perform(post("/api/v1/incidents/42/assign")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"assigneeId\":21}"))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.currentStatus").value("CLOSED"))
                .andExpect(jsonPath("$.requiredStatus[0]").value("OPEN"))
                .andExpect(jsonPath("$.requiredStatus[1]").value("ASSIGNED"));
    }

    @Test
    void hidesUnexpectedExceptionDetails() throws Exception {
        when(getIncidentService.get(42L))
                .thenThrow(new RuntimeException("database password and stacktrace marker"));

        String response = mockMvc.perform(get("/api/v1/incidents/42"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:incident-hub:problem:internal-server-error"))
                .andExpect(jsonPath("$.title").value("Internal server error"))
                .andExpect(jsonPath("$.detail").value("An unexpected error occurred"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(response)
                .doesNotContain("database password")
                .doesNotContain("stacktrace")
                .doesNotContain("RuntimeException");
    }

    private void stubSuccessfulLifecycleAction(LifecycleAction action, IncidentStatus status) {
        switch (action) {
            case START -> when(startIncidentProgressService.start(any(StartIncidentProgressCommand.class)))
                    .thenReturn(new StartIncidentProgressResult(42L, status));
            case RESOLVE -> when(resolveIncidentService.resolve(any(ResolveIncidentCommand.class)))
                    .thenReturn(new ResolveIncidentResult(42L, status));
            case CLOSE -> when(closeIncidentService.close(any(CloseIncidentCommand.class)))
                    .thenReturn(new CloseIncidentResult(42L, status));
            case REOPEN -> when(reopenIncidentService.reopen(any(ReopenIncidentCommand.class)))
                    .thenReturn(new ReopenIncidentResult(42L, status));
            case CANCEL -> when(cancelIncidentService.cancel(any(CancelIncidentCommand.class)))
                    .thenReturn(new CancelIncidentResult(42L, status));
        }
    }

    private void stubNotFound(Endpoint endpoint) {
        IncidentNotFoundException exception = new IncidentNotFoundException(42L);
        switch (endpoint) {
            case CREATE -> when(createIncidentService.create(any(CreateIncidentCommand.class)))
                    .thenThrow(new BusinessServiceNotFoundException(42L));
            case LIST -> when(listIncidentsService.execute(any(ListIncidentsQuery.class))).thenThrow(exception);
            case GET -> when(getIncidentService.get(42L)).thenThrow(exception);
            case HISTORY -> when(getIncidentHistoryService.get(42L)).thenThrow(exception);
            case ASSIGN -> when(assignIncidentService.assign(any(AssignIncidentCommand.class))).thenThrow(exception);
            case START -> when(startIncidentProgressService.start(any(StartIncidentProgressCommand.class)))
                    .thenThrow(exception);
            case RESOLVE -> when(resolveIncidentService.resolve(any(ResolveIncidentCommand.class))).thenThrow(exception);
            case CLOSE -> when(closeIncidentService.close(any(CloseIncidentCommand.class))).thenThrow(exception);
            case REOPEN -> when(reopenIncidentService.reopen(any(ReopenIncidentCommand.class))).thenThrow(exception);
            case CANCEL -> when(cancelIncidentService.cancel(any(CancelIncidentCommand.class))).thenThrow(exception);
        }
    }

    private static GetIncidentResult incident(IncidentStatus status, Long assigneeId) {
        return new GetIncidentResult(
                42L,
                "Недоступна оплата",
                "Платежи завершаются ошибкой",
                10L,
                "PAYMENTS",
                "Платежи",
                IncidentSource.MANUAL,
                IncidentPriority.HIGH,
                IncidentSeverity.SEV2,
                status,
                20L,
                "Автор",
                30L,
                "Платёжная платформа",
                "PAYMENT_PLATFORM",
                assigneeId,
                assigneeId == null ? null : "Исполнитель",
                CREATED_AT,
                UPDATED_AT
        );
    }

    private static ListIncidentItem listItem(IncidentStatus status) {
        GetIncidentResult incident = incident(status, 21L);
        return new ListIncidentItem(
                incident.id(),
                incident.title(),
                incident.affectedServiceId(),
                incident.affectedServiceCode(),
                incident.affectedServiceName(),
                incident.source(),
                incident.priority(),
                incident.severity(),
                incident.status(),
                incident.reporterId(),
                incident.reporterDisplayName(),
                incident.responsibleTeamId(),
                incident.responsibleTeamName(),
                incident.responsibleTeamCode(),
                incident.assigneeId(),
                incident.assigneeDisplayName(),
                incident.createdAt(),
                incident.updatedAt()
        );
    }

    private static Stream<Arguments> successfulLifecycleActions() {
        return Stream.of(
                Arguments.of("/api/v1/incidents/42/start", LifecycleAction.START, IncidentStatus.IN_PROGRESS),
                Arguments.of("/api/v1/incidents/42/resolve", LifecycleAction.RESOLVE, IncidentStatus.RESOLVED),
                Arguments.of("/api/v1/incidents/42/close", LifecycleAction.CLOSE, IncidentStatus.CLOSED),
                Arguments.of("/api/v1/incidents/42/reopen", LifecycleAction.REOPEN, IncidentStatus.IN_PROGRESS),
                Arguments.of("/api/v1/incidents/42/cancel", LifecycleAction.CANCEL, IncidentStatus.CANCELLED)
        );
    }

    private static Stream<Arguments> notFoundRequests() {
        return Stream.of(
                Arguments.of(post("/api/v1/incidents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_CREATE_REQUEST), Endpoint.CREATE),
                Arguments.of(get("/api/v1/incidents"), Endpoint.LIST),
                Arguments.of(get("/api/v1/incidents/42"), Endpoint.GET),
                Arguments.of(get("/api/v1/incidents/42/history"), Endpoint.HISTORY),
                Arguments.of(post("/api/v1/incidents/42/assign")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"assigneeId\":21}"), Endpoint.ASSIGN),
                Arguments.of(post("/api/v1/incidents/42/start"), Endpoint.START),
                Arguments.of(post("/api/v1/incidents/42/resolve"), Endpoint.RESOLVE),
                Arguments.of(post("/api/v1/incidents/42/close"), Endpoint.CLOSE),
                Arguments.of(post("/api/v1/incidents/42/reopen"), Endpoint.REOPEN),
                Arguments.of(post("/api/v1/incidents/42/cancel"), Endpoint.CANCEL)
        );
    }

    private static Stream<Arguments> createRequestsWithMissingRequiredField() {
        return Stream.of(
                Arguments.of("title", """
                        {"description":"d","affectedServiceId":10,"priority":"HIGH","severity":"SEV2","reporterId":20}
                        """),
                Arguments.of("description", """
                        {"title":"t","affectedServiceId":10,"priority":"HIGH","severity":"SEV2","reporterId":20}
                        """),
                Arguments.of("affectedServiceId", """
                        {"title":"t","description":"d","priority":"HIGH","severity":"SEV2","reporterId":20}
                        """),
                Arguments.of("priority", """
                        {"title":"t","description":"d","affectedServiceId":10,"severity":"SEV2","reporterId":20}
                        """),
                Arguments.of("severity", """
                        {"title":"t","description":"d","affectedServiceId":10,"priority":"HIGH","reporterId":20}
                        """),
                Arguments.of("reporterId", """
                        {"title":"t","description":"d","affectedServiceId":10,"priority":"HIGH","severity":"SEV2"}
                        """)
        );
    }

    private static Stream<Arguments> readRequestsWithInvalidId() {
        return Stream.of(
                Arguments.of(get("/api/v1/incidents/0"), ReadEndpoint.GET),
                Arguments.of(get("/api/v1/incidents/0/history"), ReadEndpoint.HISTORY)
        );
    }

    private static Stream<MockHttpServletRequestBuilder> lifecycleRequestsWithInvalidId() {
        return Stream.of(
                post("/api/v1/incidents/0/start"),
                post("/api/v1/incidents/0/resolve"),
                post("/api/v1/incidents/0/close"),
                post("/api/v1/incidents/0/reopen"),
                post("/api/v1/incidents/0/cancel")
        );
    }

    private enum LifecycleAction {
        START,
        RESOLVE,
        CLOSE,
        REOPEN,
        CANCEL
    }

    private enum Endpoint {
        CREATE,
        LIST,
        GET,
        HISTORY,
        ASSIGN,
        START,
        RESOLVE,
        CLOSE,
        REOPEN,
        CANCEL
    }

    private enum ReadEndpoint {
        GET,
        HISTORY
    }
}
