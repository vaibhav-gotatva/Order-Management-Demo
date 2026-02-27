package com.assignment.demo.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String BEARER_SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI orderManagementOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Order Management API")
                        .description(
                                "REST API for managing trade orders. " +
                                "Protected endpoints require a JWT Bearer token obtained from POST /api/auth/login.")
                        .version("1.0.0")
                        .contact(new Contact().name("Order Management Team")))
                .components(new Components()
                        .addSecuritySchemes(BEARER_SCHEME_NAME,
                                new SecurityScheme()
                                        .name(BEARER_SCHEME_NAME)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Paste the JWT token (without the 'Bearer ' prefix)")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME_NAME));
    }
}
