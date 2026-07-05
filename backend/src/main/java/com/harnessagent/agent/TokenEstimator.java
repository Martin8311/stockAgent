package com.harnessagent.agent;

import org.springframework.stereotype.Component;

@Component
public class TokenEstimator {

    public int estimate(String text) {
        if (text == null || text.isBlank()) {
            return 0;
        }
        long cjkCount = text.chars()
                .filter(this::isCjk)
                .count();
        int nonCjkCount = Math.max(0, text.length() - (int) cjkCount);
        return Math.max(1, (int) cjkCount + (int) Math.ceil(nonCjkCount / 4.0));
    }

    private boolean isCjk(int codePoint) {
        return (codePoint >= 0x4E00 && codePoint <= 0x9FFF)
                || (codePoint >= 0x3400 && codePoint <= 0x4DBF)
                || (codePoint >= 0x3040 && codePoint <= 0x30FF)
                || (codePoint >= 0xAC00 && codePoint <= 0xD7AF);
    }
}
