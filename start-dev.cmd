@echo off
setlocal
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\start-dev.ps1" %*
set EXIT_CODE=%ERRORLEVEL%
echo.
if not "%HARNESS_AGENT_NO_PAUSE%"=="1" (
  if not "%EXIT_CODE%"=="0" (
    echo [harness-agent] Startup failed with exit code %EXIT_CODE%.
    echo [harness-agent] Check .dev\logs\start-dev.log for details.
  ) else (
    echo [harness-agent] Startup command finished. Services may still be running in the background.
  )
  echo.
  pause
)
endlocal
exit /b %EXIT_CODE%
