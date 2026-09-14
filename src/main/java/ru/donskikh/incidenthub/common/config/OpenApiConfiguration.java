package ru.donskikh.incidenthub.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.http.MediaType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.method.HandlerMethod;

@Configuration
public class OpenApiConfiguration {

    public static final String BEARER_AUTH = "bearerAuth";

    @Bean
    public OpenAPI incidentHubOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("IncidentHub API")
                        .description("REST API for managing service ownership, dependencies, and incident lifecycle")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("IncidentHub repository")
                                .url("https://github.com/6yJlka/incident-hub")))
                .components(new Components().addSecuritySchemes(
                        BEARER_AUTH,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                ))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH));
    }

    @Bean
    public OperationCustomizer securityErrorOperationCustomizer() {
        return (operation, handlerMethod) -> {
            if (!isPublicOperation(handlerMethod)) {
                addAuthenticationError(operation);
            }
            if (isRoleProtectedOperation(handlerMethod)) {
                addAuthorizationError(operation);
            }
            return operation;
        };
    }

    private boolean isPublicOperation(HandlerMethod handlerMethod) {
        return handlerMethod.hasMethodAnnotation(SecurityRequirements.class)
                || AnnotatedElementUtils.hasAnnotation(handlerMethod.getBeanType(), SecurityRequirements.class);
    }

    private boolean isRoleProtectedOperation(HandlerMethod handlerMethod) {
        return AnnotatedElementUtils.hasAnnotation(handlerMethod.getMethod(), PreAuthorize.class)
                || AnnotatedElementUtils.hasAnnotation(handlerMethod.getBeanType(), PreAuthorize.class);
    }

    private void addAuthenticationError(Operation operation) {
        operation.getResponses().addApiResponse(
                "401",
                new ApiResponse()
                        .description("Authentication credentials are missing or invalid")
                        .content(new Content().addMediaType(
                                MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                                new io.swagger.v3.oas.models.media.MediaType()
                                        .schema(new Schema<>().$ref("#/components/schemas/ProblemDetail"))
                        ))
        );
    }

    private void addAuthorizationError(Operation operation) {
        operation.getResponses().addApiResponse(
                "403",
                new ApiResponse()
                        .description("The authenticated user does not have the required role")
                        .content(new Content().addMediaType(
                                MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                                new io.swagger.v3.oas.models.media.MediaType()
                                        .schema(new Schema<>().$ref("#/components/schemas/ProblemDetail"))
                        ))
        );
    }
}
