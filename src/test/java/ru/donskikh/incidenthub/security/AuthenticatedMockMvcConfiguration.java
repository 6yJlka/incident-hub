package ru.donskikh.incidenthub.security;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.MockMvcBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import ru.donskikh.incidenthub.identity.UserRole;

import java.util.List;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@TestConfiguration(proxyBeanMethods = false)
public class AuthenticatedMockMvcConfiguration {

    @Bean
    MockMvcBuilderCustomizer authenticatedDefaultRequest() {
        AuthenticatedUser principal = new AuthenticatedUser(
                42L,
                "mvc-test@example.com",
                UserRole.REPORTER
        );
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_REPORTER"))
        );
        return builder -> builder.defaultRequest(get("/").with(authentication(authentication)));
    }
}
