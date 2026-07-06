@echo off
setlocal

powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\stop-all.ps1" %*
set EXIT_CODE=%ERRORLEVEL%

echo.
if not "%HARNESS_AGENT_NO_PAUSE%"=="1" (
  if not "%EXIT_CODE%"=="0" (
    echo [harness-agent] Stop-all failed with exit code %EXIT_CODE%.
  ) else (
    echo [harness-agent] Stop-all command finished.
  )
  echo.
  pause
)

endlocal
exit /b %EXIT_CODE%
