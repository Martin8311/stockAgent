package com.harnessagent.security;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security")
public record SecurityProperties(
        List<String> allowedOrigins,
        String jwtSecret,
        long tokenTtlMinutes
) {

    public SecurityProperties {
        if (allowedOrigins == null || allowedOrigins.isEmpty()) {
            allowedOrigins = List.of("http://localhost:5173");
        }
        if (jwtSecret == null || jwtSecret.isBlank()) {
            jwtSecret = "dev-only-change-me-please-32-bytes-minimum";
        }
        if (tokenTtlMinutes <= 0) {
            tokenTtlMinutes = 120;
        }
    }
}
