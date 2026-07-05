package com.harnessagent.security;

import java.time.Instant;
import java.util.Set;

public record JwtClaims(
        Long userId,
        String subject,
        Set<String> roles,
        Instant issuedAt,
        Instant expiresAt
) {
}

