package ru.donskikh.incidenthub.alert.web;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.donskikh.incidenthub.alert.application.AcceptAlertsService;
import ru.donskikh.incidenthub.alert.application.AcceptAlertsResult;
import ru.donskikh.incidenthub.alert.application.AlertAcceptanceResult;
import ru.donskikh.incidenthub.alert.application.AlertAcceptanceStatus;
import ru.donskikh.incidenthub.alert.application.AlertRejectionReason;
import ru.donskikh.incidenthub.security.SecurityConfiguration;

import java.util.List;
import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AlertController.class, properties = "app.alerts.max-batch-size=2")
@Import({SecurityConfiguration.class, AlertWebMapper.class})
class AlertControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AcceptAlertsService service;


    private static final String ITEM = """
            {"source":"prometheus","externalId":"one","serviceCode":"CORE_DATABASE",
             "signalType":"LATENCY","severity":"WARNING","occurredAt":"2026-10-18T12:00:00Z",
             "description":"Latency increased"}
            """;

    @Test
    void returnsResultsInOriginalOrder() throws Exception {
        when(service.accept(any())).thenReturn(new AcceptAlertsResult(List.of(
                new AlertAcceptanceResult("one", AlertAcceptanceStatus.ACCEPTED, 42L, null),
                new AlertAcceptanceResult("two", AlertAcceptanceStatus.REJECTED, null, AlertRejectionReason.UNKNOWN_SERVICE)
        )));

        mockMvc.perform(post("/api/v1/alerts").header("X-Integration-Token", "test-integration-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"alerts\":[" + ITEM + "," + ITEM.replace("one", "two") + "]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results[0].externalId").value("one"))
                .andExpect(jsonPath("$.results[0].status").value("ACCEPTED"))
                .andExpect(jsonPath("$.results[0].alertId").value(42))
                .andExpect(jsonPath("$.results[0].reason").doesNotExist())
                .andExpect(jsonPath("$.results[1].externalId").value("two"))
                .andExpect(jsonPath("$.results[1].status").value("REJECTED"))
                .andExpect(jsonPath("$.results[1].reason").value("UNKNOWN_SERVICE"));
    }

    @ParameterizedTest
    @MethodSource("invalidBatches")
    void rejectsInvalidBatchBeforeCallingService(String body) throws Exception {
        mockMvc.perform(post("/api/v1/alerts").header("X-Integration-Token", "test-integration-token")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400));
        verifyNoInteractions(service);
    }

    @Test
    void returnsFutureOccurrenceRejectionAlongsideAcceptedItem() throws Exception {
        when(service.accept(any())).thenReturn(new AcceptAlertsResult(List.of(
                new AlertAcceptanceResult("one", AlertAcceptanceStatus.REJECTED, null, AlertRejectionReason.OCCURRED_IN_FUTURE),
                new AlertAcceptanceResult("one", AlertAcceptanceStatus.ACCEPTED, 42L, null)
        )));

        mockMvc.perform(post("/api/v1/alerts").header("X-Integration-Token", "test-integration-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"alerts\":[" + ITEM + "," + ITEM + "]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results[0].status").value("REJECTED"))
                .andExpect(jsonPath("$.results[0].reason").value("OCCURRED_IN_FUTURE"))
                .andExpect(jsonPath("$.results[0].alertId").doesNotExist())
                .andExpect(jsonPath("$.results[1].status").value("ACCEPTED"))
                .andExpect(jsonPath("$.results[1].alertId").value(42));
    }

    private static Stream<String> invalidBatches() {
        return Stream.of(
                "{}", "{\"alerts\":null}", "{\"alerts\":[]}", "{\"alerts\":[null]}", ITEM,
                "{\"alerts\":[" + ITEM + "," + ITEM + "," + ITEM + "]}",
                "{\"alerts\":[" + ITEM.replace("LATENCY", "UNKNOWN") + "]}",
                "{\"alerts\":[" + ITEM.replace("WARNING", "SEV1") + "]}",
                "{\"alerts\":[" + ITEM.replace("\"source\":\"prometheus\",", "") + "]}",
                "{\"alerts\":[" + ITEM.replace("\"occurredAt\":\"2026-10-18T12:00:00Z\",", "") + "]}",
                "{\"alerts\":[" + ITEM.replace("Latency increased", " ") + "]}"
        );
    }
}
