package ru.donskikh.incidenthub.security;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.MockMvcBuilderCustomizer;
import org.springframework.context.annotation.Bean;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@TestConfiguration(proxyBeanMethods = false)
public class AuthenticatedMockMvcConfiguration {

    @Bean
    MockMvcBuilderCustomizer authenticatedDefaultRequest() {
        return builder -> builder.defaultRequest(get("/").with(user("mvc-test").roles("REPORTER")));
    }
}
