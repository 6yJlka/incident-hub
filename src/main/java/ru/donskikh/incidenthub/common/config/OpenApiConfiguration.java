package ru.donskikh.incidenthub.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfiguration {

    @Bean
    public OpenAPI incidentHubOpenApi() {
        return new OpenAPI().info(new Info()
                .title("IncidentHub API")
                .description("REST API for managing service ownership, dependencies, and incident lifecycle")
                .version("1.0.0")
                .contact(new Contact()
                        .name("IncidentHub repository")
                        .url("https://github.com/6yJlka/incident-hub")));
    }
}
