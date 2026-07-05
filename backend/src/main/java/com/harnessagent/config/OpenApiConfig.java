package com.harnessagent.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI harnessAgentOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Harness Engineering Intelligent Assistant API")
                        .version("0.1.0")
                        .description("APIs for investment research assistance, portfolio management, compliance, and audit.")
                        .license(new License().name("Private project for portfolio demonstration")));
    }
}

