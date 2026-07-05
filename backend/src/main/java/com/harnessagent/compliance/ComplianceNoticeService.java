package com.harnessagent.compliance;

import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ComplianceNoticeService {

    private final ComplianceProperties complianceProperties;

    public ComplianceNoticeService(ComplianceProperties complianceProperties) {
        this.complianceProperties = complianceProperties;
    }

    public ComplianceNotice getDefaultNotice() {
        return new ComplianceNotice(
                "Investment Research Compliance Notice",
                "Educational explanation, auxiliary analysis, and risk reminders.",
                complianceProperties.defaultDisclaimer(),
                List.copyOf(complianceProperties.requiredWarnings())
        );
    }
}

