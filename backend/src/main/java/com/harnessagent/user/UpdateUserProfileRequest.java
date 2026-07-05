package com.harnessagent.user;

public record UpdateUserProfileRequest(
        RiskPreference riskPreference,
        InvestmentHorizon investmentHorizon,
        CapitalPurpose capitalPurpose
) {
}

