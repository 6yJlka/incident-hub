package ru.donskikh.incidenthub.alertgroup.web;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.donskikh.incidenthub.alertgroup.AlertGroupNotFoundException;
import ru.donskikh.incidenthub.alertgroup.AlertGroupStatus;
import ru.donskikh.incidenthub.alertgroup.application.AlertGroupItem;
import ru.donskikh.incidenthub.alertgroup.application.GetAlertGroupQuery;
import ru.donskikh.incidenthub.alertgroup.application.GetAlertGroupResult;
import ru.donskikh.incidenthub.alertgroup.application.GetAlertGroupService;
import ru.donskikh.incidenthub.alertgroup.application.ListAlertGroupsQuery;
import ru.donskikh.incidenthub.alertgroup.application.ListAlertGroupsResult;
import ru.donskikh.incidenthub.alertgroup.application.ListAlertGroupsService;
import ru.donskikh.incidenthub.security.AuthenticatedMockMvcConfiguration;
import ru.donskikh.incidenthub.security.SecurityConfiguration;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AlertGroupController.class)
@Import({SecurityConfiguration.class, AlertGroupWebMapper.class, AuthenticatedMockMvcConfiguration.class,
        AvailableAlertGroupActions.class, ru.donskikh.incidenthub.incident.web.IncidentActionAuthorization.class})
class AlertGroupControllerTest {

    @Test
    void confirmsAndReturnsAvailableActions() throws Exception {
        when(confirmService.confirm(any())).thenReturn(new GetAlertGroupResult(item(), List.of()));
        mockMvc.perform(post("/api/v1/alert-groups/7/confirm").contentType("application/json")
                        .content("{\"severity\":\"SEV2\",\"priority\":\"HIGH\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableActions[0]").value("CONFIRM"));
    }

    @Test
    void dismissAcceptsMissingBody() throws Exception {
        when(dismissService.dismiss(any())).thenReturn(new GetAlertGroupResult(item(), List.of()));
        mockMvc.perform(post("/api/v1/alert-groups/7/dismiss"))
                .andExpect(status().isOk());
        verify(dismissService).dismiss(org.mockito.ArgumentMatchers.argThat(command -> command.reason() == null));
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"severity\":\"SEV2\"}", "{\"priority\":\"HIGH\"}",
            "{\"severity\":\"CRITICAL\",\"priority\":\"HIGH\"}",
            "{\"severity\":\"SEV2\",\"priority\":\"HIGH\",\"businessServiceId\":0}",
            "{\"severity\":\"SEV2\",\"priority\":\"HIGH\",\"title\":\" \"}"})
    void rejectsInvalidConfirmation(String body) throws Exception {
        mockMvc.perform(post("/api/v1/alert-groups/7/confirm").contentType("application/json").content(body))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(confirmService);
    }

    @Test
    void rejectsLongDismissReason() throws Exception {
        mockMvc.perform(post("/api/v1/alert-groups/7/dismiss").contentType("application/json")
                        .content("{\"reason\":\"" + "x".repeat(2001) + "\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(dismissService);
    }

    @Test
    void distinguishesStateAndVersionProblems() throws Exception {
        when(confirmService.confirm(any())).thenThrow(new ru.donskikh.incidenthub.alertgroup.AlertGroupStateConflictException(
                AlertGroupStatus.CONFIRMED));
        mockMvc.perform(post("/api/v1/alert-groups/7/confirm").contentType("application/json")
                        .content("{\"severity\":\"SEV2\",\"priority\":\"HIGH\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("urn:incident-hub:problem:conflict"))
                .andExpect(jsonPath("$.currentStatus").value("CONFIRMED"));
        when(dismissService.dismiss(any())).thenThrow(
                new ru.donskikh.incidenthub.alertgroup.AlertGroupConcurrentModificationException(7, new RuntimeException()));
        mockMvc.perform(post("/api/v1/alert-groups/7/dismiss"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("urn:incident-hub:problem:concurrent-modification"))
                .andExpect(jsonPath("$.alertGroupId").value(7));
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ListAlertGroupsService listService;

    @MockitoBean
    private GetAlertGroupService getService;

    @MockitoBean
    private ru.donskikh.incidenthub.alertgroup.application.ConfirmAlertGroupService confirmService;

    @MockitoBean
    private ru.donskikh.incidenthub.alertgroup.application.DismissAlertGroupService dismissService;

    @Test
    void returnsStandardListFormatAndPassesFilters() throws Exception {
        when(listService.execute(any())).thenReturn(new ListAlertGroupsResult(
                List.of(item()), 1, 5, 8, 2, false, true));

        mockMvc.perform(get("/api/v1/alert-groups").param("page", "1").param("size", "5")
                        .param("status", "OPEN").param("rootServiceId", "42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(7))
                .andExpect(jsonPath("$.items[0].probableRootServiceCode").value("CORE_DATABASE"))
                .andExpect(jsonPath("$.page").value(1)).andExpect(jsonPath("$.size").value(5))
                .andExpect(jsonPath("$.totalElements").value(8)).andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.hasNext").value(false)).andExpect(jsonPath("$.hasPrevious").value(true))
                .andExpect(jsonPath("$.content").doesNotExist());
        verify(listService).execute(new ListAlertGroupsQuery(1, 5, AlertGroupStatus.OPEN, 42L));
    }

    @Test
    void returnsGroupCardWithAlerts() throws Exception {
        when(getService.get(any())).thenReturn(new GetAlertGroupResult(item(), List.of()));

        mockMvc.perform(get("/api/v1/alert-groups/7")).andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.alerts").isArray());
        verify(getService).get(new GetAlertGroupQuery(7));
    }

    @Test
    void returnsProblemDetailForUnknownGroup() throws Exception {
        when(getService.get(any())).thenThrow(new AlertGroupNotFoundException(7));
        mockMvc.perform(get("/api/v1/alert-groups/7")).andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.type").value("urn:incident-hub:problem:not-found"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"?page=-1", "?size=0", "?size=101", "?status=UNKNOWN", "?rootServiceId=0", "?rootServiceId=abc"})
    void rejectsInvalidListQueriesBeforeServiceCall(String query) throws Exception {
        mockMvc.perform(get("/api/v1/alert-groups" + query)).andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"));
        verifyNoInteractions(listService, getService);
    }

    @Test
    void rejectsInvalidGroupIdentifier() throws Exception {
        mockMvc.perform(get("/api/v1/alert-groups/0")).andExpect(status().isBadRequest());
        verifyNoInteractions(listService, getService);
    }

    private AlertGroupItem item() {
        return new AlertGroupItem(7L, AlertGroupStatus.OPEN, 42L, "CORE_DATABASE", "Database",
                Instant.parse("2026-10-18T12:00:00Z"), Instant.parse("2026-10-18T12:05:00Z"), null);
    }
}
