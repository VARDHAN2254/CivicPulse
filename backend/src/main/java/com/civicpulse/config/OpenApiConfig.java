package com.civicpulse.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "BearerAuth";

    @Bean
    public OpenAPI civicPulseOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("CivicPulse — Community Event & Engagement Platform API")
                        .description("Production REST API for CivicPulse providing Event Management, Waitlists, QR Attendance, Discussions, and Moderation.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Jai Sai Vardhan Reddy")
                                .email("contact@civicpulse.org"))
                        .license(new License()
                                .name("All Rights Reserved - Copyright © 2026 Jai Sai Vardhan Reddy")))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME,
                                new SecurityScheme()
                                        .name(SECURITY_SCHEME_NAME)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Enter your JWT Bearer token to authenticate requests.")));
    }
}
