package ru.donskikh.incidenthub.security;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.MockMvcBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import ru.donskikh.incidenthub.identity.UserRole;

import java.util.List;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@TestConfiguration(proxyBeanMethods = false)
public class AuthenticatedMockMvcConfiguration {

    @Bean
    MockMvcBuilderCustomizer authenticatedDefaultRequest() {
        return builder -> builder.defaultRequest(get("/").with(authenticatedAs(UserRole.ADMIN)));
    }

    public static RequestPostProcessor authenticatedAs(UserRole role) {
        AuthenticatedUser principal = new AuthenticatedUser(42L, "mvc-test@example.com", role);
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + role.name()))
        );
        return authentication(authentication);
    }
}
