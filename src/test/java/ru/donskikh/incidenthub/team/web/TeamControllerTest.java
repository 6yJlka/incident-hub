package ru.donskikh.incidenthub.team.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.donskikh.incidenthub.common.web.GlobalExceptionHandler;
import ru.donskikh.incidenthub.team.TeamCodeAlreadyExistsException;
import ru.donskikh.incidenthub.team.application.CreateTeamCommand;
import ru.donskikh.incidenthub.team.application.CreateTeamResult;
import ru.donskikh.incidenthub.team.application.CreateTeamService;
import ru.donskikh.incidenthub.team.application.ListTeamItem;
import ru.donskikh.incidenthub.team.application.ListTeamsQuery;
import ru.donskikh.incidenthub.team.application.ListTeamsResult;
import ru.donskikh.incidenthub.team.application.ListTeamsService;

import java.time.Instant;
import java.util.List;

import static org.hamcrest.Matchers.hasItem;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TeamController.class)
@Import({TeamWebMapper.class, GlobalExceptionHandler.class})
class TeamControllerTest {

    private static final String VALID_REQUEST = """
            {
              "code": "platform",
              "name": "Platform"
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreateTeamService createTeamService;

    @MockitoBean
    private ListTeamsService listTeamsService;

    @Test
    void createsTeam() throws Exception {
        when(createTeamService.create(any(CreateTeamCommand.class)))
                .thenReturn(new CreateTeamResult(7L, "PLATFORM", true));

        mockMvc.perform(post("/api/v1/teams")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_REQUEST))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/v1/teams/7"))
                .andExpect(jsonPath("$.teamId").value(7))
                .andExpect(jsonPath("$.code").value("PLATFORM"))
                .andExpect(jsonPath("$.active").value(true));

        verify(createTeamService).create(new CreateTeamCommand("platform", "Platform"));
    }

    @Test
    void listsTeamsWithFilterAndPagination() throws Exception {
        Instant createdAt = Instant.parse("2026-09-01T10:00:00Z");
        Instant updatedAt = Instant.parse("2026-09-02T11:00:00Z");
        when(listTeamsService.execute(any(ListTeamsQuery.class))).thenReturn(new ListTeamsResult(
                List.of(new ListTeamItem(7L, "PLATFORM", "Platform", true, createdAt, updatedAt)),
                1, 5, 8, 2, false, true
        ));

        mockMvc.perform(get("/api/v1/teams")
                        .param("page", "1")
                        .param("size", "5")
                        .param("active", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(7))
                .andExpect(jsonPath("$.items[0].code").value("PLATFORM"))
                .andExpect(jsonPath("$.items[0].createdAt").value(createdAt.toString()))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(5))
                .andExpect(jsonPath("$.totalElements").value(8))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.hasNext").value(false))
                .andExpect(jsonPath("$.hasPrevious").value(true));

        verify(listTeamsService).execute(new ListTeamsQuery(1, 5, true));
    }

    @Test
    void reportsEveryMissingRequiredCreateField() throws Exception {
        mockMvc.perform(post("/api/v1/teams")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.errors[*].field", hasItem("code")))
                .andExpect(jsonPath("$.errors[*].field", hasItem("name")));
    }

    @Test
    void rejectsInvalidListQuery() throws Exception {
        mockMvc.perform(get("/api/v1/teams").param("size", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("size must be greater than 0"));
    }

    @Test
    void mapsDuplicateCodeToConflict() throws Exception {
        when(createTeamService.create(any(CreateTeamCommand.class)))
                .thenThrow(new TeamCodeAlreadyExistsException("PLATFORM"));

        mockMvc.perform(post("/api/v1/teams")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_REQUEST))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Team code already exists: PLATFORM"));
    }
}
