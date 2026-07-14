package com.icop.user.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger metadata. The bearerAuth scheme declared here is what the
 * controllers reference with @SecurityRequirement — it gives the swagger UI
 * its "Authorize" button so you can paste a JWT and try the locked endpoints.
 */
@Configuration
@OpenAPIDefinition(info = @Info(title = "User Service API", version = "1.0", description = "Authentication and user management"))
@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
public class OpenApiConfig {}
