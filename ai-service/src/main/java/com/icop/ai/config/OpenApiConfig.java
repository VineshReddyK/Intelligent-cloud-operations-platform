package com.icop.ai.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "ICOP AI Service API",
                version = "1.0.0",
                description = "AI-powered anomaly detection, failure prediction, and auto-remediation for ICOP microservices"
        )
)
public class OpenApiConfig {}
