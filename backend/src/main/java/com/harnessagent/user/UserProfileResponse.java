package com.harnessagent.user;

import java.time.Instant;
import java.util.Set;

public record UserProfileResponse(
        Long id,
        String email,
        String displayName,
        UserStatus status,
        Set<AppRole> roles,
        RiskPreference riskPreference,
        InvestmentHorizon investmentHorizon,
        CapitalPurpose capitalPurpose,
        Instant createdAt,
        Instant updatedAt
) {
    public static UserProfileResponse from(AppUser user, UserProfile profile) {
        return new UserProfileResponse(
                user.getId(),
                user.getEmail(),
                user.getDisplayName(),
                user.getStatus(),
                user.getRoles(),
                profile.getRiskPreference(),
                profile.getInvestmentHorizon(),
                profile.getCapitalPurpose(),
                user.getCreatedAt(),
                profile.getUpdatedAt()
        );
    }
}

