package com.harnessagent.compliance;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.compliance")
public record ComplianceProperties(
        String defaultDisclaimer,
        List<String> requiredWarnings
) {
    public ComplianceProperties {
        if (defaultDisclaimer == null || defaultDisclaimer.isBlank()) {
            defaultDisclaimer = "This system provides educational analysis only and does not replace licensed financial advice.";
        }
        if (requiredWarnings == null || requiredWarnings.isEmpty()) {
            requiredWarnings = List.of(
                    "Investment involves risk.",
                    "Historical performance does not guarantee future results.",
                    "No output from this system should be interpreted as a promise of return."
            );
        }
    }
}

