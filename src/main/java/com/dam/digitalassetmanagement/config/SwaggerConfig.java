package com.dam.digitalassetmanagement.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springdoc.core.models.GroupedOpenApi;

import java.util.List;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Digital Asset Management (DAM) System API")
                        .version("1.0.0")
                        .description("""
                                RESTful API for managing digital assets including images, videos, and audio files.
                                
                                Features:
                                - User Roles & Permissions (Admin, Uploader, Editor, Viewer)
                                - Asset Upload & Versioning
                                - Metadata Tagging & Custom Fields
                                - Advanced Search & Filtering
                                - Asset Preview & Watermarking
                                - Download & Usage Tracking
                                - Collection/Folder Management
                                - API Access for external applications
                                - Audit Logs & Activity Tracking
                                - Usage Reports Generation
                                """)
                        .contact(new Contact()
                                .name("DAM Team")
                                .email("support@dam-system.com")
                                .url("https://dam-system.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0.html")))
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("Development Server"),
                        new Server().url("https://api.dam-system.com").description("Production Server")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Enter JWT token obtained from /api/auth/login endpoint")));
    }

    @Bean
    public GroupedOpenApi publicApi() {
        return GroupedOpenApi.builder()
                .group("public")
                .pathsToMatch("/api/**")
                .build();
    }
}