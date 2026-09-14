package ru.donskikh.incidenthub.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import ru.donskikh.incidenthub.PostgreSQLIntegrationTest;
import ru.donskikh.incidenthub.identity.UserRole;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityIntegrationTest extends PostgreSQLIntegrationTest {

    private static final AuthenticatedUser USER = new AuthenticatedUser(
            42L, "engineer@example.com", UserRole.ENGINEER
    );
    private static final AuthenticatedUser REPORTER = new AuthenticatedUser(
            43L, "reporter@example.com", UserRole.REPORTER
    );
    private static final AuthenticatedUser ADMIN = new AuthenticatedUser(
            44L, "admin@example.com", UserRole.ADMIN
    );

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenService jwtTokenService;

    @Autowired
    private JwtProperties jwtProperties;

    @Autowired
    private Clock clock;

    @Test
    void rejectsProtectedRequestWithoutToken() throws Exception {
        mockMvc.perform(get("/api/v1/incidents"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:incident-hub:problem:authentication-required"));
    }

    @Test
    void acceptsProtectedRequestWithValidToken() throws Exception {
        String token = jwtTokenService.issue(USER, clock.instant()).value();

        mockMvc.perform(get("/api/v1/incidents")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void rejectsExpiredToken() throws Exception {
        Instant expiredIssueTime = clock.instant().minus(jwtProperties.accessTokenTtl()).minusSeconds(1);
        String token = jwtTokenService.issue(USER, expiredIssueTime).value();

        mockMvc.perform(get("/api/v1/incidents")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsTokenSignedWithAnotherSecret() throws Exception {
        JwtProperties otherProperties = new JwtProperties(
                "YW5vdGhlci1qd3Qtc2VjcmV0LXRoYXQtaXMtYXQtbGVhc3QtMzItYnl0ZXM=",
                Duration.ofHours(1),
                false
        );
        String token = new JwtTokenService(otherProperties, clock).issue(USER, clock.instant()).value();

        mockMvc.perform(get("/api/v1/incidents")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void returnsNeutralProblemDetailWhenRoleIsInsufficient() throws Exception {
        String token = jwtTokenService.issue(REPORTER, clock.instant()).value();

        mockMvc.perform(post("/api/v1/incidents/42/start")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:incident-hub:problem:access-denied"))
                .andExpect(jsonPath("$.title").value("Access denied"))
                .andExpect(jsonPath("$.detail").value("You do not have permission to perform this action"))
                .andExpect(jsonPath("$.instance").value("/api/v1/incidents/42/start"));
    }

    @Test
    void acceptsCorsPreflightFromConfiguredOrigin() throws Exception {
        mockMvc.perform(options("/api/v1/incidents")
                        .header(HttpHeaders.ORIGIN, "http://localhost:5173")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Authorization, Content-Type"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5173"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS,
                        org.hamcrest.Matchers.containsString("POST")))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS,
                        org.hamcrest.Matchers.containsStringIgnoringCase("authorization")));
    }

    @Test
    void rejectsCorsPreflightFromForeignOrigin() throws Exception {
        mockMvc.perform(options("/api/v1/incidents")
                        .header(HttpHeaders.ORIGIN, "https://foreign.example")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }

    @Test
    @Transactional
    void administratorCreatesLoginEnabledUserWithSelectedRole() throws Exception {
        String token = jwtTokenService.issue(ADMIN, clock.instant()).value();

        mockMvc.perform(post("/api/v1/users")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "stage4-engineer@example.com",
                                  "displayName": "Stage 4 Engineer",
                                  "password": "secure-password",
                                  "role": "ENGINEER"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("ENGINEER"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "stage4-engineer@example.com",
                                  "password": "secure-password"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty());
    }

    @Test
    void exposesAllPublicPathsWithoutToken() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/swagger-ui.html"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/swagger-ui/index.html"));
        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk());
    }
}
