package ru.donskikh.incidenthub.identity.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.donskikh.incidenthub.common.web.GlobalExceptionHandler;
import ru.donskikh.incidenthub.identity.UserEmailAlreadyExistsException;
import ru.donskikh.incidenthub.identity.application.CreateUserCommand;
import ru.donskikh.incidenthub.identity.application.CreateUserResult;
import ru.donskikh.incidenthub.identity.application.CreateUserService;
import ru.donskikh.incidenthub.identity.application.ListUserItem;
import ru.donskikh.incidenthub.identity.application.ListUsersQuery;
import ru.donskikh.incidenthub.identity.application.ListUsersResult;
import ru.donskikh.incidenthub.identity.application.ListUsersService;

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

@WebMvcTest(UserController.class)
@Import({UserWebMapper.class, GlobalExceptionHandler.class})
class UserControllerTest {

    private static final String VALID_REQUEST = """
            {
              "email": "user@example.com",
              "displayName": "Example User"
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreateUserService createUserService;

    @MockitoBean
    private ListUsersService listUsersService;

    @Test
    void createsUserAndAcceptsTrimmedEmail() throws Exception {
        when(createUserService.create(any(CreateUserCommand.class)))
                .thenReturn(new CreateUserResult(9L, "user@example.com", true));

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "  User@Example.COM  ",
                                  "displayName": "Example User"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/v1/users/9"))
                .andExpect(jsonPath("$.userId").value(9))
                .andExpect(jsonPath("$.email").value("user@example.com"))
                .andExpect(jsonPath("$.active").value(true));

        verify(createUserService).create(new CreateUserCommand("User@Example.COM", "Example User"));
    }

    @Test
    void listsUsersWithFilterAndPagination() throws Exception {
        Instant createdAt = Instant.parse("2026-09-01T10:00:00Z");
        Instant updatedAt = Instant.parse("2026-09-02T11:00:00Z");
        when(listUsersService.execute(any(ListUsersQuery.class))).thenReturn(new ListUsersResult(
                List.of(new ListUserItem(
                        9L, "user@example.com", "Example User", true, createdAt, updatedAt
                )),
                1, 5, 8, 2, false, true
        ));

        mockMvc.perform(get("/api/v1/users")
                        .param("page", "1")
                        .param("size", "5")
                        .param("active", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(9))
                .andExpect(jsonPath("$.items[0].email").value("user@example.com"))
                .andExpect(jsonPath("$.items[0].createdAt").value(createdAt.toString()))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(5))
                .andExpect(jsonPath("$.totalElements").value(8))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.hasNext").value(false))
                .andExpect(jsonPath("$.hasPrevious").value(true));

        verify(listUsersService).execute(new ListUsersQuery(1, 5, true));
    }

    @Test
    void reportsEveryMissingRequiredCreateField() throws Exception {
        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.errors[*].field", hasItem("email")))
                .andExpect(jsonPath("$.errors[*].field", hasItem("displayName")));
    }

    @Test
    void rejectsMalformedEmailAfterTrimming() throws Exception {
        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "  not-an-email  ",
                                  "displayName": "Example User"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[*].field", hasItem("email")));
    }

    @Test
    void rejectsInvalidListQuery() throws Exception {
        mockMvc.perform(get("/api/v1/users").param("size", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("size must be greater than 0"));
    }

    @Test
    void mapsDuplicateEmailToConflict() throws Exception {
        when(createUserService.create(any(CreateUserCommand.class)))
                .thenThrow(new UserEmailAlreadyExistsException("user@example.com"));

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_REQUEST))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("User email already exists: user@example.com"));
    }
}
