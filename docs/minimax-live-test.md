# MiniMax Live Test Guide

This project keeps normal automated tests deterministic by default. `mvn test` uses mock AI responses and does not call Ollama or MiniMax.

Use the MiniMax live path only for local smoke tests or demos when `MINIMAX_API_KEY` is already configured in your local environment.

## Local App Demo

Start the stack with the MiniMax test profile:

```powershell
$env:SPRING_PROFILES_ACTIVE="minimax-test"
powershell -NoProfile -ExecutionPolicy Bypass -File scripts\start-dev.ps1 -UseH2
```

Then open `http://localhost:5173`, choose `MiniMax paid model`, and ask the analysis question in Chinese.

## Spring Boot Profile

The backend also provides `minimax-test`:

```powershell
$env:SPRING_PROFILES_ACTIVE="local-h2,minimax-test"
powershell -NoProfile -ExecutionPolicy Bypass -File scripts\start-dev.ps1 -UseH2
```

This profile:

- enables `test-mode` so token cost is recorded as free for test/demo usage;
- disables `mock-responses-enabled` so the HTTP MiniMax gateway is used;
- sets `default-model-id` to `minimax-chat`;
- sets `response-language` to `zh-CN`;
- switches compliance disclaimer and required warnings to Chinese.

## Optional Live Smoke Test

The live MiniMax test is skipped unless explicitly enabled:

```powershell
$env:MINIMAX_LIVE_TEST_ENABLED="true"
$env:AI_RESPONSE_LANGUAGE="zh-CN"
cd D:\harness-agent\backend
mvn -Dtest=MiniMaxLiveSmokeTest test
```

Expected behavior:

- `/api/ai/analysis` uses `minimax-chat`;
- token usage source is `ACTUAL`;
- `testMode` remains `true`, so the request is not marked billable inside this app;
- the natural-language JSON values contain Chinese text;
- JSON field names remain English for DTO compatibility.

Do not enable this test in CI unless a real MiniMax key, network access, rate limits, and cost controls are intentionally configured.
