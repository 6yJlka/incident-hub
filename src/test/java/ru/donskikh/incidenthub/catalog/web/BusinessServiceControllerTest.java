package ru.donskikh.incidenthub.catalog.web;

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
import ru.donskikh.incidenthub.catalog.BusinessServiceCodeAlreadyExistsException;
import ru.donskikh.incidenthub.catalog.BusinessServiceNotFoundException;
import ru.donskikh.incidenthub.catalog.DependencyType;
import ru.donskikh.incidenthub.catalog.ServiceDependencyAlreadyExistsException;
import ru.donskikh.incidenthub.catalog.ServiceDependencyNotFoundException;
import ru.donskikh.incidenthub.catalog.ServiceSelfDependencyNotAllowedException;
import ru.donskikh.incidenthub.catalog.ServiceTier;
import ru.donskikh.incidenthub.catalog.application.AddServiceDependencyCommand;
import ru.donskikh.incidenthub.catalog.application.AddServiceDependencyResult;
import ru.donskikh.incidenthub.catalog.application.AddServiceDependencyService;
import ru.donskikh.incidenthub.catalog.application.AffectedServiceItem;
import ru.donskikh.incidenthub.catalog.application.CreateBusinessServiceCommand;
import ru.donskikh.incidenthub.catalog.application.CreateBusinessServiceResult;
import ru.donskikh.incidenthub.catalog.application.CreateBusinessServiceService;
import ru.donskikh.incidenthub.catalog.application.DirectServiceDependencyItem;
import ru.donskikh.incidenthub.catalog.application.GetAffectedServicesQuery;
import ru.donskikh.incidenthub.catalog.application.GetAffectedServicesResult;
import ru.donskikh.incidenthub.catalog.application.GetAffectedServicesService;
import ru.donskikh.incidenthub.catalog.application.GetBusinessServiceQuery;
import ru.donskikh.incidenthub.catalog.application.GetBusinessServiceResult;
import ru.donskikh.incidenthub.catalog.application.GetBusinessServiceService;
import ru.donskikh.incidenthub.catalog.application.ListBusinessServiceItem;
import ru.donskikh.incidenthub.catalog.application.ListBusinessServicesQuery;
import ru.donskikh.incidenthub.catalog.application.ListBusinessServicesResult;
import ru.donskikh.incidenthub.catalog.application.ListBusinessServicesService;
import ru.donskikh.incidenthub.catalog.application.RemoveServiceDependencyCommand;
import ru.donskikh.incidenthub.catalog.application.RemoveServiceDependencyResult;
import ru.donskikh.incidenthub.catalog.application.RemoveServiceDependencyService;
import ru.donskikh.incidenthub.common.web.GlobalExceptionHandler;
import ru.donskikh.incidenthub.team.TeamNotFoundException;

import java.time.Instant;
import java.util.List;
import java.util.stream.Stream;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BusinessServiceController.class)
@Import({BusinessServiceWebMapper.class, GlobalExceptionHandler.class})
class BusinessServiceControllerTest {

    private static final String VALID_CREATE_REQUEST = """
            {
              "code": "payments",
              "name": "Payments",
              "description": "Payment processing",
              "ownerTeamId": 30,
              "tier": "TIER_1"
            }
            """;

    private static final Instant CREATED_AT = Instant.parse("2026-09-11T08:00:00Z");
    private static final Instant UPDATED_AT = Instant.parse("2026-09-11T08:05:00Z");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreateBusinessServiceService createBusinessServiceService;

    @MockitoBean
    private ListBusinessServicesService listBusinessServicesService;

    @MockitoBean
    private GetBusinessServiceService getBusinessServiceService;

    @MockitoBean
    private GetAffectedServicesService getAffectedServicesService;

    @MockitoBean
    private AddServiceDependencyService addServiceDependencyService;

    @MockitoBean
    private RemoveServiceDependencyService removeServiceDependencyService;

    @Test
    void createsBusinessService() throws Exception {
        when(createBusinessServiceService.create(any(CreateBusinessServiceCommand.class)))
                .thenReturn(new CreateBusinessServiceResult(42L, "PAYMENTS", ServiceTier.TIER_1, true));

        mockMvc.perform(post("/api/v1/services")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_CREATE_REQUEST))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/v1/services/42"))
                .andExpect(jsonPath("$.businessServiceId").value(42))
                .andExpect(jsonPath("$.code").value("PAYMENTS"))
                .andExpect(jsonPath("$.tier").value("TIER_1"))
                .andExpect(jsonPath("$.active").value(true));

        verify(createBusinessServiceService).create(new CreateBusinessServiceCommand(
                "payments",
                "Payments",
                "Payment processing",
                30L,
                ServiceTier.TIER_1
        ));
    }

    @Test
    void listsBusinessServicesWithFiltersAndPagination() throws Exception {
        when(listBusinessServicesService.execute(any(ListBusinessServicesQuery.class)))
                .thenReturn(new ListBusinessServicesResult(
                        List.of(listItem()),
                        1,
                        5,
                        8,
                        2,
                        false,
                        true
                ));

        mockMvc.perform(get("/api/v1/services")
                        .param("page", "1")
                        .param("size", "5")
                        .param("ownerTeamId", "30")
                        .param("tier", "TIER_1")
                        .param("active", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(42))
                .andExpect(jsonPath("$.items[0].code").value("PAYMENTS"))
                .andExpect(jsonPath("$.items[0].ownerTeamCode").value("PAYMENT_PLATFORM"))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(5))
                .andExpect(jsonPath("$.totalElements").value(8))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.hasNext").value(false))
                .andExpect(jsonPath("$.hasPrevious").value(true))
                .andExpect(jsonPath("$", not(hasKey("content"))));

        verify(listBusinessServicesService).execute(new ListBusinessServicesQuery(
                1,
                5,
                30L,
                ServiceTier.TIER_1,
                true
        ));
    }

    @Test
    void getsBusinessServiceWithDirectDependencies() throws Exception {
        when(getBusinessServiceService.get(any(GetBusinessServiceQuery.class)))
                .thenReturn(businessService());

        mockMvc.perform(get("/api/v1/services/42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(42))
                .andExpect(jsonPath("$.code").value("PAYMENTS"))
                .andExpect(jsonPath("$.ownerTeamId").value(30))
                .andExpect(jsonPath("$.dependencies[0].relationshipId").value(100))
                .andExpect(jsonPath("$.dependencies[0].serviceId").value(41))
                .andExpect(jsonPath("$.dependencies[0].type").value("SYNC"));

        verify(getBusinessServiceService).get(new GetBusinessServiceQuery(42L));
    }

    @Test
    void getsAffectedServicesWithDefaultDepth() throws Exception {
        when(getAffectedServicesService.get(any(GetAffectedServicesQuery.class)))
                .thenReturn(new GetAffectedServicesResult(
                        List.of(new AffectedServiceItem(
                                43L,
                                "CHECKOUT",
                                "Checkout",
                                ServiceTier.TIER_1,
                                true,
                                30L,
                                "Payment Platform",
                                1,
                                DependencyType.SYNC
                        )),
                        42L,
                        10
                ));

        mockMvc.perform(get("/api/v1/services/42/affected"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.businessServiceId").value(42))
                .andExpect(jsonPath("$.maxDepth").value(10))
                .andExpect(jsonPath("$.items[0].id").value(43))
                .andExpect(jsonPath("$.items[0].depth").value(1))
                .andExpect(jsonPath("$.items[0].dependencyType").value("SYNC"));

        verify(getAffectedServicesService).get(new GetAffectedServicesQuery(42L, 10));
    }

    @Test
    void addsServiceDependency() throws Exception {
        when(addServiceDependencyService.add(any(AddServiceDependencyCommand.class)))
                .thenReturn(new AddServiceDependencyResult(
                        100L,
                        42L,
                        41L,
                        DependencyType.SYNC,
                        CREATED_AT
                ));

        mockMvc.perform(post("/api/v1/services/42/dependencies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dependencyServiceId\":41,\"type\":\"SYNC\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.relationshipId").value(100))
                .andExpect(jsonPath("$.dependentServiceId").value(42))
                .andExpect(jsonPath("$.dependencyServiceId").value(41))
                .andExpect(jsonPath("$.type").value("SYNC"))
                .andExpect(jsonPath("$.createdAt").value(CREATED_AT.toString()));

        verify(addServiceDependencyService).add(new AddServiceDependencyCommand(
                42L,
                41L,
                DependencyType.SYNC
        ));
    }

    @Test
    void removesServiceDependencyWithEmptyBody() throws Exception {
        when(removeServiceDependencyService.remove(any(RemoveServiceDependencyCommand.class)))
                .thenReturn(new RemoveServiceDependencyResult(42L, 41L));

        mockMvc.perform(delete("/api/v1/services/42/dependencies/41"))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(removeServiceDependencyService).remove(new RemoveServiceDependencyCommand(42L, 41L));
    }

    @ParameterizedTest
    @MethodSource("notFoundRequests")
    void mapsNotFoundForEveryEndpoint(
            MockHttpServletRequestBuilder request,
            Endpoint endpoint
    ) throws Exception {
        stubNotFound(endpoint);

        mockMvc.perform(request)
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:incident-hub:problem:not-found"))
                .andExpect(jsonPath("$.title").value("Resource not found"))
                .andExpect(jsonPath("$.instance").value(request.buildRequest(null).getRequestURI()));
    }

    @ParameterizedTest(name = "missing {0}")
    @MethodSource("createRequestsWithMissingRequiredField")
    void rejectsEachMissingRequiredCreateField(String field, String requestBody) throws Exception {
        mockMvc.perform(post("/api/v1/services")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:incident-hub:problem:validation"))
                .andExpect(jsonPath("$.errors[*].field", hasItem(field)));
    }

    @Test
    void rejectsInvalidListQuery() throws Exception {
        mockMvc.perform(get("/api/v1/services").param("size", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("size must be greater than 0"));
    }

    @Test
    void rejectsInvalidBusinessServiceIdForGet() throws Exception {
        mockMvc.perform(get("/api/v1/services/0"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("businessServiceId must be positive"));
    }

    @ParameterizedTest
    @MethodSource("invalidAffectedDepths")
    void rejectsAffectedDepthOutsideAllowedRange(String maxDepth, String detail) throws Exception {
        mockMvc.perform(get("/api/v1/services/42/affected").param("maxDepth", maxDepth))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value(detail));
    }

    @Test
    void rejectsInvalidAddDependencyBody() throws Exception {
        mockMvc.perform(post("/api/v1/services/42/dependencies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.errors[*].field", hasItem("dependencyServiceId")))
                .andExpect(jsonPath("$.errors[*].field", hasItem("type")));
    }

    @Test
    void rejectsInvalidDependentServiceIdWhenAddingDependency() throws Exception {
        mockMvc.perform(post("/api/v1/services/0/dependencies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dependencyServiceId\":41,\"type\":\"SYNC\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("dependentServiceId must be positive"));
    }

    @Test
    void rejectsInvalidDependencyIdWhenRemovingDependency() throws Exception {
        mockMvc.perform(delete("/api/v1/services/42/dependencies/0"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("dependencyServiceId must be positive"));
    }

    @Test
    void mapsDuplicateBusinessServiceCodeToConflict() throws Exception {
        when(createBusinessServiceService.create(any(CreateBusinessServiceCommand.class)))
                .thenThrow(new BusinessServiceCodeAlreadyExistsException("PAYMENTS"));

        mockMvc.perform(post("/api/v1/services")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_CREATE_REQUEST))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Business service code already exists: PAYMENTS"));
    }

    @Test
    void mapsDuplicateServiceDependencyToConflict() throws Exception {
        when(addServiceDependencyService.add(any(AddServiceDependencyCommand.class)))
                .thenThrow(new ServiceDependencyAlreadyExistsException(42L, 41L));

        mockMvc.perform(post("/api/v1/services/42/dependencies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dependencyServiceId\":41,\"type\":\"SYNC\"}"))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Service dependency already exists: 42 -> 41"));
    }

    @Test
    void mapsSelfDependencyToConflict() throws Exception {
        when(addServiceDependencyService.add(any(AddServiceDependencyCommand.class)))
                .thenThrow(new ServiceSelfDependencyNotAllowedException(42L));

        mockMvc.perform(post("/api/v1/services/42/dependencies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dependencyServiceId\":42,\"type\":\"SYNC\"}"))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Business service cannot depend on itself: 42"));
    }

    private void stubNotFound(Endpoint endpoint) {
        BusinessServiceNotFoundException serviceException = new BusinessServiceNotFoundException(42L);
        switch (endpoint) {
            case CREATE -> when(createBusinessServiceService.create(any(CreateBusinessServiceCommand.class)))
                    .thenThrow(new TeamNotFoundException(30L));
            case LIST -> when(listBusinessServicesService.execute(any(ListBusinessServicesQuery.class)))
                    .thenThrow(serviceException);
            case GET -> when(getBusinessServiceService.get(any(GetBusinessServiceQuery.class)))
                    .thenThrow(serviceException);
            case AFFECTED -> when(getAffectedServicesService.get(any(GetAffectedServicesQuery.class)))
                    .thenThrow(serviceException);
            case ADD_DEPENDENCY -> when(addServiceDependencyService.add(any(AddServiceDependencyCommand.class)))
                    .thenThrow(serviceException);
            case REMOVE_DEPENDENCY -> when(removeServiceDependencyService.remove(
                            any(RemoveServiceDependencyCommand.class)))
                    .thenThrow(new ServiceDependencyNotFoundException(42L, 41L));
        }
    }

    private static GetBusinessServiceResult businessService() {
        return new GetBusinessServiceResult(
                42L,
                "PAYMENTS",
                "Payments",
                "Payment processing",
                30L,
                "Payment Platform",
                "PAYMENT_PLATFORM",
                ServiceTier.TIER_1,
                true,
                CREATED_AT,
                UPDATED_AT,
                List.of(new DirectServiceDependencyItem(
                        100L,
                        41L,
                        "DATABASE",
                        "Database",
                        ServiceTier.TIER_1,
                        true,
                        DependencyType.SYNC,
                        CREATED_AT
                ))
        );
    }

    private static ListBusinessServiceItem listItem() {
        return new ListBusinessServiceItem(
                42L,
                "PAYMENTS",
                "Payments",
                30L,
                "Payment Platform",
                "PAYMENT_PLATFORM",
                ServiceTier.TIER_1,
                true,
                CREATED_AT,
                UPDATED_AT
        );
    }

    private static Stream<Arguments> notFoundRequests() {
        return Stream.of(
                Arguments.of(post("/api/v1/services")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_CREATE_REQUEST), Endpoint.CREATE),
                Arguments.of(get("/api/v1/services"), Endpoint.LIST),
                Arguments.of(get("/api/v1/services/42"), Endpoint.GET),
                Arguments.of(get("/api/v1/services/42/affected"), Endpoint.AFFECTED),
                Arguments.of(post("/api/v1/services/42/dependencies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dependencyServiceId\":41,\"type\":\"SYNC\"}"), Endpoint.ADD_DEPENDENCY),
                Arguments.of(delete("/api/v1/services/42/dependencies/41"), Endpoint.REMOVE_DEPENDENCY)
        );
    }

    private static Stream<Arguments> createRequestsWithMissingRequiredField() {
        return Stream.of(
                Arguments.of("code", """
                        {"name":"Payments","description":"d","ownerTeamId":30,"tier":"TIER_1"}
                        """),
                Arguments.of("name", """
                        {"code":"PAYMENTS","description":"d","ownerTeamId":30,"tier":"TIER_1"}
                        """),
                Arguments.of("description", """
                        {"code":"PAYMENTS","name":"Payments","ownerTeamId":30,"tier":"TIER_1"}
                        """),
                Arguments.of("ownerTeamId", """
                        {"code":"PAYMENTS","name":"Payments","description":"d","tier":"TIER_1"}
                        """),
                Arguments.of("tier", """
                        {"code":"PAYMENTS","name":"Payments","description":"d","ownerTeamId":30}
                        """)
        );
    }

    private static Stream<Arguments> invalidAffectedDepths() {
        return Stream.of(
                Arguments.of("0", "maxDepth must be greater than 0"),
                Arguments.of("11", "maxDepth must not exceed 10")
        );
    }

    private enum Endpoint {
        CREATE,
        LIST,
        GET,
        AFFECTED,
        ADD_DEPENDENCY,
        REMOVE_DEPENDENCY
    }
}
