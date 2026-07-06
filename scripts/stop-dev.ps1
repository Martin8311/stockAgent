[CmdletBinding()]
param(
    [switch]$StopDocker
)

$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

$RootDir = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$DevDir = Join-Path $RootDir ".dev"
$ProjectProcessNames = @("java.exe", "node.exe", "cmd.exe")

function Write-Step {
    param([string]$Message)
    Write-Host "[harness-agent] $Message"
}

function Get-ChildProcessInfo {
    param([int]$ParentProcessId)

    try {
        return @(Get-CimInstance Win32_Process -Filter "ParentProcessId = $ParentProcessId" -ErrorAction Stop)
    } catch {
        Write-Warning "Could not inspect child processes for PID ${ParentProcessId}: $($_.Exception.Message)"
        return @()
    }
}

function Invoke-TaskKillTree {
    param([int]$RootProcessId)

    $previousErrorActionPreference = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    try {
        $null = & taskkill.exe /PID $RootProcessId /T /F 2>&1
        $exitCode = $LASTEXITCODE
    } finally {
        $ErrorActionPreference = $previousErrorActionPreference
    }

    return $exitCode -eq 0
}

function Stop-ProcessTree {
    param([int]$RootProcessId)

    $process = Get-Process -Id $RootProcessId -ErrorAction SilentlyContinue
    if ($process -and (Invoke-TaskKillTree -RootProcessId $RootProcessId)) {
        return 1
    }

    $stopped = 0
    foreach ($child in Get-ChildProcessInfo -ParentProcessId $RootProcessId) {
        $stopped += Stop-ProcessTree -RootProcessId ([int]$child.ProcessId)
    }

    $process = Get-Process -Id $RootProcessId -ErrorAction SilentlyContinue
    if ($process) {
        Stop-Process -Id $process.Id -Force
        $stopped += 1
    }

    return $stopped
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

    $stopped = Stop-ProcessTree -RootProcessId ([int]$rawPid)
    if ($stopped -gt 0) {
        Write-Step "Stopped $Name process tree rooted at PID $rawPid ($stopped process(es))."
    } else {
        Write-Step "$Name process tree rooted at PID $rawPid was not running."
    }

    Remove-Item $PidFile -Force
}

function Stop-OrphanedProjectProcesses {
    try {
        $processes = @(Get-CimInstance Win32_Process -ErrorAction Stop | Where-Object {
            $_.CommandLine -and
            $_.CommandLine.Contains($RootDir) -and
            ($ProjectProcessNames -contains $_.Name)
        })
    } catch {
        Write-Warning "Could not inspect orphaned project processes: $($_.Exception.Message)"
        return
    }

    foreach ($process in $processes) {
        $stopped = Stop-ProcessTree -RootProcessId ([int]$process.ProcessId)
        if ($stopped -gt 0) {
            Write-Step "Stopped orphaned project process tree rooted at PID $($process.ProcessId) ($stopped process(es))."
        }
    }
}

Stop-TrackedProcess -Name "Backend" -PidFile (Join-Path $DevDir "backend.pid")
Stop-TrackedProcess -Name "Frontend" -PidFile (Join-Path $DevDir "frontend.pid")
Stop-OrphanedProjectProcesses

if ($StopDocker) {
    Write-Step "Stopping MySQL Docker Compose service ..."
    if (!(Get-Command docker -ErrorAction SilentlyContinue)) {
        Write-Warning "Docker CLI was not found. Skipping Docker Compose stop."
    } else {
        Push-Location $RootDir
        try {
            docker compose stop mysql
            if ($LASTEXITCODE -ne 0) {
                Write-Warning "Docker Compose stop returned exit code $LASTEXITCODE."
            }
        } catch {
            Write-Warning "Could not stop Docker Compose service: $($_.Exception.Message)"
        } finally {
            Pop-Location
        }
    }
}

Write-Step "Done."
