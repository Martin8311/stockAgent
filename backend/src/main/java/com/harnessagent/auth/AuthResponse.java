package com.harnessagent.auth;

import com.harnessagent.user.AppRole;
import java.time.Instant;
import java.util.Set;

public record AuthResponse(
        String tokenType,
        String accessToken,
        Instant expiresAt,
        UserAccountResponse user
) {
    public record UserAccountResponse(
            Long id,
            String email,
            String displayName,
            Set<AppRole> roles
    ) {
    }
}

