package com.harnessagent.compliance;

import java.util.List;

public record ComplianceNotice(
        String title,
        String allowedUse,
        String limitations,
        List<String> requiredDisclosures
) {
}

