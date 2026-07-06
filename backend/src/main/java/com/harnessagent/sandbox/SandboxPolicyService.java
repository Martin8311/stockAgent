package com.harnessagent.sandbox;

import com.harnessagent.audit.RiskLevel;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;

@Service
public class SandboxPolicyService {

    private static final int DEFAULT_TIMEOUT_MS = 1200;
    private static final int MIN_TIMEOUT_MS = 100;
    private static final int MAX_TIMEOUT_MS = 3000;
    private static final int MAX_SCRIPT_LENGTH = 4000;

    private static final List<String> DANGEROUS_KEYWORDS = List.of(
            "runtime",
            "processbuilder",
            "powershell",
            "cmd.exe",
            "bash",
            "exec",
            "system.",
            "socket",
            "http://",
            "https://",
            "file:",
            "../",
            "$env",
            "userprofile",
            "api_key",
            "apikey",
            "secret",
            "delete from",
            "drop table",
            "truncate table",
            "import ",
            "require(",
            "fetch("
    );

    private static final List<String> HUMAN_APPROVAL_KEYWORDS = List.of(
            "leverage",
            "margin",
            "short",
            "options",
            "futures",
            "real trade",
            "real_trade",
            "external api",
            "paid data",
            "live market",
            "\u6760\u6746",
            "\u878d\u8d44",
            "\u878d\u5238",
            "\u671f\u6743",
            "\u671f\u8d27",
            "\u5b9e\u76d8",
            "\u771f\u5b9e\u4ea4\u6613",
            "\u5916\u90e8\u63a5\u53e3"
    );

    public SandboxPolicyDecision evaluate(SandboxTaskRequest request) {
        String normalizedScript = normalizeScript(request.script());
        int timeoutMs = normalizeTimeout(request.timeoutMs());
        if (normalizedScript.length() > MAX_SCRIPT_LENGTH) {
            return reject("Sandbox script exceeds maximum length.", timeoutMs, normalizedScript);
        }
        String lower = normalizedScript.toLowerCase(Locale.ROOT);
        if (DANGEROUS_KEYWORDS.stream().anyMatch(lower::contains)) {
            return reject("Sandbox script contains blocked system, network, file, or secret access keywords.", timeoutMs, normalizedScript);
        }
        if (HUMAN_APPROVAL_KEYWORDS.stream().anyMatch(lower::contains)) {
            return new SandboxPolicyDecision(
                    false,
                    true,
                    RiskLevel.HIGH,
                    "Sandbox task references leveraged, external, paid, or real-trading behavior and requires human approval.",
                    timeoutMs,
                    normalizedScript
            );
        }
        RiskLevel riskLevel = request.taskType() == SandboxTaskType.PORTFOLIO_STRESS_TEST
                ? RiskLevel.MEDIUM
                : RiskLevel.LOW;
        return new SandboxPolicyDecision(false, false, riskLevel, null, timeoutMs, normalizedScript);
    }

    private SandboxPolicyDecision reject(String reason, int timeoutMs, String normalizedScript) {
        return new SandboxPolicyDecision(true, false, RiskLevel.CRITICAL, reason, timeoutMs, normalizedScript);
    }

    private int normalizeTimeout(Integer timeoutMs) {
        if (timeoutMs == null) {
            return DEFAULT_TIMEOUT_MS;
        }
        if (timeoutMs < MIN_TIMEOUT_MS) {
            return MIN_TIMEOUT_MS;
        }
        return Math.min(timeoutMs, MAX_TIMEOUT_MS);
    }

    private String normalizeScript(String script) {
        if (script == null) {
            return "";
        }
        return script.trim();
    }
}
