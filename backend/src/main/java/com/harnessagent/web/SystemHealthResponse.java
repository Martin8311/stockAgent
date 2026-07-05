package com.harnessagent.web;

import java.time.Instant;

public record SystemHealthResponse(
        String service,
        String phase,
        String status,
        boolean complianceGuardEnabled,
        Instant timestamp
) {
}

