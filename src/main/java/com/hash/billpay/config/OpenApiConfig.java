package com.hash.billpay.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI configuration that adds the X-API-Key security scheme to Swagger UI.
 * This allows users to enter their API key directly in the Swagger interface.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        final String securitySchemeName = "ApiKeyAuth";

        return new OpenAPI()
                .info(new Info()
                        .title("Bill Payments Service API")
                        .version("1.0.0")
                        .description("Production-grade bill payment purchase transaction management API. "
                                + "All /api/** endpoints require a valid X-API-Key header."))
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName, new SecurityScheme()
                                .name("X-API-Key")
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .description("API key for accessing bill payment endpoints")));
    }
}