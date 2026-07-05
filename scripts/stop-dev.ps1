[CmdletBinding()]
param(
    [switch]$StopDocker
)

$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

$RootDir = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$DevDir = Join-Path $RootDir ".dev"

function Write-Step {
    param([string]$Message)
    Write-Host "[harness-agent] $Message"
}

function Stop-TrackedProcess {
    param(
        [string]$Name,
        [string]$PidFile
    )

    if (!(Test-Path $PidFile)) {
        Write-Step "$Name PID file not found."
        return
    }

    $rawPid = (Get-Content $PidFile -ErrorAction SilentlyContinue | Select-Object -First 1)
    if ([string]::IsNullOrWhiteSpace($rawPid)) {
        Remove-Item $PidFile -Force
        Write-Step "$Name PID file was empty and has been removed."
        return
    }

    $process = Get-Process -Id ([int]$rawPid) -ErrorAction SilentlyContinue
    if ($process) {
        Stop-Process -Id $process.Id -Force
        Write-Step "Stopped $Name with PID $($process.Id)."
    } else {
        Write-Step "$Name process with PID $rawPid was not running."
    }

    Remove-Item $PidFile -Force
}

Stop-TrackedProcess -Name "Backend" -PidFile (Join-Path $DevDir "backend.pid")
Stop-TrackedProcess -Name "Frontend" -PidFile (Join-Path $DevDir "frontend.pid")

if ($StopDocker) {
    Write-Step "Stopping MySQL Docker Compose service ..."
    Push-Location $RootDir
    try {
        docker compose stop mysql
    } finally {
        Pop-Location
    }
}

Write-Step "Done."

