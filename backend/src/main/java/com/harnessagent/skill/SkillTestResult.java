package com.harnessagent.skill;

import java.time.Instant;
import java.util.List;

public record SkillTestResult(
        boolean passed,
        Long sandboxTaskId,
        String sandboxStatus,
        String summary,
        List<String> checks,
        List<String> riskWarnings,
        Instant testedAt
) {
}
