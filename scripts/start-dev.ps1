[CmdletBinding()]
param(
    [switch]$SkipDocker,
    [switch]$InstallFrontend,
    [switch]$NoWait,
    [string]$JwtSecret = $env:JWT_SECRET
)

$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

$RootDir = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$BackendDir = Join-Path $RootDir "backend"
$FrontendDir = Join-Path $RootDir "frontend"
$DevDir = Join-Path $RootDir ".dev"
$LogDir = Join-Path $DevDir "logs"
$BackendPidFile = Join-Path $DevDir "backend.pid"
$FrontendPidFile = Join-Path $DevDir "frontend.pid"
$BackendRunScript = Join-Path $DevDir "run-backend.ps1"
$FrontendRunScript = Join-Path $DevDir "run-frontend.ps1"
$ServerPort = if ($env:SERVER_PORT) { $env:SERVER_PORT } else { "8080" }
$FrontendPort = "5173"

New-Item -ItemType Directory -Force -Path $DevDir, $LogDir | Out-Null

function Write-Step {
    param([string]$Message)
    Write-Host "[harness-agent] $Message"
}

function New-DevSecret {
    $bytes = New-Object byte[] 32
    [System.Security.Cryptography.RandomNumberGenerator]::Fill($bytes)
    return [Convert]::ToBase64String($bytes)
}

function Get-MavenPath {
    $mvnCommand = Get-Command mvn.cmd -ErrorAction SilentlyContinue
    if ($mvnCommand) {
        return $mvnCommand.Source
    }

    $mvnCommand = Get-Command mvn -ErrorAction SilentlyContinue
    if ($mvnCommand) {
        return $mvnCommand.Source
    }

    $knownMaven = Join-Path $HOME ".m2\wrapper\dists\apache-maven-3.9.11\03d7e36a140982eea48e22c1dcac01d8862b2550b2939e09a0809bbc5182a5bc\bin\mvn.cmd"
    if (Test-Path $knownMaven) {
        return $knownMaven
    }

    throw "Maven was not found. Install Maven, add it to PATH, or restore the cached Maven distribution under $HOME\.m2\wrapper\dists."
}

function Get-TrackedProcess {
    param([string]$PidFile)

    if (!(Test-Path $PidFile)) {
        return $null
    }

    $rawPid = (Get-Content $PidFile -ErrorAction SilentlyContinue | Select-Object -First 1)
    if ([string]::IsNullOrWhiteSpace($rawPid)) {
        return $null
    }

    return Get-Process -Id ([int]$rawPid) -ErrorAction SilentlyContinue
}

function Wait-Http {
    param(
        [string]$Name,
        [string]$Url,
        [int]$TimeoutSeconds
    )

    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    while ((Get-Date) -lt $deadline) {
        try {
            $response = Invoke-WebRequest -Uri $Url -UseBasicParsing -TimeoutSec 3
            if ($response.StatusCode -ge 200 -and $response.StatusCode -lt 500) {
                Write-Step "$Name is ready: $Url"
                return
            }
        } catch {
            Start-Sleep -Seconds 2
        }
    }

    Write-Warning "$Name did not become ready within $TimeoutSeconds seconds. Check logs under $LogDir."
}

function Wait-DockerHealthy {
    param(
        [string]$ContainerName,
        [int]$TimeoutSeconds
    )

    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    while ((Get-Date) -lt $deadline) {
        $status = docker inspect --format "{{.State.Health.Status}}" $ContainerName 2>$null
        if ($LASTEXITCODE -eq 0 -and $status -eq "healthy") {
            Write-Step "$ContainerName is healthy."
            return
        }
        Start-Sleep -Seconds 2
    }

    Write-Warning "$ContainerName did not become healthy within $TimeoutSeconds seconds. Backend startup may fail if MySQL is not ready."
}

function Start-Backend {
    $running = Get-TrackedProcess -PidFile $BackendPidFile
    if ($running) {
        Write-Step "Backend already appears to be running with PID $($running.Id)."
        return
    }

    $mavenPath = Get-MavenPath
    $effectiveSecret = if ([string]::IsNullOrWhiteSpace($JwtSecret)) { New-DevSecret } else { $JwtSecret }

    $backendLog = Join-Path $LogDir "backend.log"
    $backendErr = Join-Path $LogDir "backend.err.log"
    $escapedSecret = $effectiveSecret.Replace("'", "''")
    $escapedMaven = $mavenPath.Replace("'", "''")
    $escapedPom = (Join-Path $BackendDir "pom.xml").Replace("'", "''")

    @"
`$ErrorActionPreference = "Stop"
`$env:JWT_SECRET = '$escapedSecret'
`$env:SERVER_PORT = '$ServerPort'
& '$escapedMaven' -f '$escapedPom' spring-boot:run
"@ | Set-Content -Path $BackendRunScript -Encoding UTF8

    Write-Step "Starting backend on http://localhost:$ServerPort ..."
    $process = Start-Process `
        -FilePath "powershell.exe" `
        -ArgumentList @("-NoProfile", "-ExecutionPolicy", "Bypass", "-File", $BackendRunScript) `
        -WorkingDirectory $BackendDir `
        -RedirectStandardOutput $backendLog `
        -RedirectStandardError $backendErr `
        -WindowStyle Hidden `
        -PassThru
    Set-Content -Path $BackendPidFile -Value $process.Id -Encoding ASCII
    Write-Step "Backend PID $($process.Id), log: $backendLog"
}

function Start-Frontend {
    $running = Get-TrackedProcess -PidFile $FrontendPidFile
    if ($running) {
        Write-Step "Frontend already appears to be running with PID $($running.Id)."
        return
    }

    $frontendLog = Join-Path $LogDir "frontend.log"
    $frontendErr = Join-Path $LogDir "frontend.err.log"
    $shouldInstall = $InstallFrontend -or !(Test-Path (Join-Path $FrontendDir "node_modules"))
    $installBlock = if ($shouldInstall) { "cmd /c npm install --cache .npm-cache`r`nif (`$LASTEXITCODE -ne 0) { exit `$LASTEXITCODE }" } else { "" }

    @"
`$ErrorActionPreference = "Stop"
$installBlock
cmd /c npm run dev
"@ | Set-Content -Path $FrontendRunScript -Encoding UTF8

    Write-Step "Starting frontend on http://localhost:5173 ..."
    $process = Start-Process `
        -FilePath "powershell.exe" `
        -ArgumentList @("-NoProfile", "-ExecutionPolicy", "Bypass", "-File", $FrontendRunScript) `
        -WorkingDirectory $FrontendDir `
        -RedirectStandardOutput $frontendLog `
        -RedirectStandardError $frontendErr `
        -WindowStyle Hidden `
        -PassThru
    Set-Content -Path $FrontendPidFile -Value $process.Id -Encoding ASCII
    Write-Step "Frontend PID $($process.Id), log: $frontendLog"
}

if (!(Test-Path $BackendDir) -or !(Test-Path $FrontendDir)) {
    throw "Run this script from the repository checkout. Expected backend and frontend directories under $RootDir."
}

if (!$SkipDocker) {
    if (!(Get-Command docker -ErrorAction SilentlyContinue)) {
        throw "Docker was not found. Install Docker Desktop or rerun with -SkipDocker if MySQL is already available."
    }

    Write-Step "Starting MySQL with Docker Compose ..."
    Push-Location $RootDir
    try {
        docker compose up -d mysql
        if ($LASTEXITCODE -ne 0) {
            throw "docker compose up failed with exit code $LASTEXITCODE"
        }
        if (!$NoWait) {
            Wait-DockerHealthy -ContainerName "harness-agent-mysql" -TimeoutSeconds 90
        }
    } finally {
        Pop-Location
    }
}

Start-Backend
Start-Frontend

if (!$NoWait) {
    Wait-Http -Name "Backend" -Url "http://localhost:$ServerPort/api/public/system/health" -TimeoutSeconds 90
    Wait-Http -Name "Frontend" -Url "http://localhost:$FrontendPort" -TimeoutSeconds 60
}

Write-Host ""
Write-Step "Development stack is starting."
Write-Host "Frontend:  http://localhost:$FrontendPort"
Write-Host "Backend:   http://localhost:$ServerPort"
Write-Host "Swagger:   http://localhost:$ServerPort/swagger-ui.html"
Write-Host "Logs:      $LogDir"
Write-Host "Stop:      powershell -NoProfile -ExecutionPolicy Bypass -File scripts\stop-dev.ps1"
