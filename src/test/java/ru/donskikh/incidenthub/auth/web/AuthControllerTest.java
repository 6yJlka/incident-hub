package ru.donskikh.incidenthub.auth.web;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import ru.donskikh.incidenthub.auth.InvalidCredentialsException;
import ru.donskikh.incidenthub.auth.application.LoginCommand;
import ru.donskikh.incidenthub.auth.application.LoginService;
import ru.donskikh.incidenthub.auth.application.RegisterUserCommand;
import ru.donskikh.incidenthub.auth.application.RegisterUserResult;
import ru.donskikh.incidenthub.auth.application.RegisterUserService;
import ru.donskikh.incidenthub.common.web.GlobalExceptionHandler;
import ru.donskikh.incidenthub.identity.UserRole;
import ru.donskikh.incidenthub.security.SecurityConfiguration;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import({AuthWebMapper.class, GlobalExceptionHandler.class, SecurityConfiguration.class})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RegisterUserService registerUserService;

    @MockitoBean
    private LoginService loginService;

    @Test
    void publicRegistrationPathIsAccessibleWithoutTokenInMvcSlice() throws Exception {
        when(registerUserService.register(any(RegisterUserCommand.class))).thenReturn(new RegisterUserResult(
                21L, "reporter@example.com", "Example Reporter", UserRole.REPORTER, true
        ));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "reporter@example.com",
                                  "displayName": "Example Reporter",
                                  "password": "secure-password"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("REPORTER"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void securityAndLoginFailuresUseTheSameProblemDetailContract() throws Exception {
        MvcResult securityFailure = mockMvc.perform(get("/api/v1/protected-resource"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andReturn();

        when(loginService.login(any(LoginCommand.class))).thenThrow(new InvalidCredentialsException());
        MvcResult loginFailure = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "missing@example.com",
                                  "password": "wrong-password"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andReturn();

        Map<String, Object> securityProblem = JsonPath.read(securityFailure.getResponse().getContentAsString(), "$");
        Map<String, Object> loginProblem = JsonPath.read(loginFailure.getResponse().getContentAsString(), "$");

        assertThat(securityProblem.keySet()).containsExactlyInAnyOrderElementsOf(loginProblem.keySet());
        assertThat(securityProblem).containsEntry("type", loginProblem.get("type"));
        assertThat(securityProblem).containsEntry("title", loginProblem.get("title"));
        assertThat(securityProblem).containsEntry("status", loginProblem.get("status"));
        assertThat(securityProblem).containsEntry("detail", loginProblem.get("detail"));
    }
}
