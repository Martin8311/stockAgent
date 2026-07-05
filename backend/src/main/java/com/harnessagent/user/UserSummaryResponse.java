package com.harnessagent.user;

import java.time.Instant;
import java.util.Set;

public record UserSummaryResponse(
        Long id,
        String email,
        String displayName,
        UserStatus status,
        Set<AppRole> roles,
        Instant createdAt
) {
    public static UserSummaryResponse from(AppUser user) {
        return new UserSummaryResponse(
                user.getId(),
                user.getEmail(),
                user.getDisplayName(),
                user.getStatus(),
                user.getRoles(),
                user.getCreatedAt()
        );
    }
}

