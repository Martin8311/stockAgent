[CmdletBinding()]
param(
    [switch]$KeepDocker
)

$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

$RootDir = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$StopDevScript = Join-Path $PSScriptRoot "stop-dev.ps1"

function Write-Step {
    param([string]$Message)
    Write-Host "[harness-agent] $Message"
}

if (!(Test-Path $StopDevScript)) {
    throw "Could not find stop-dev script at $StopDevScript"
}

Write-Step "Stopping all Harness Agent services under $RootDir ..."

$stopArgs = @()
if (!$KeepDocker) {
    $stopArgs += "-StopDocker"
}

& $StopDevScript @stopArgs

if ($KeepDocker) {
    Write-Step "Docker services were left running because -KeepDocker was provided."
} else {
    Write-Step "Docker MySQL stop was requested. Use docker compose ps to confirm container state if needed."
}

Write-Step "All stop requests have been issued."
