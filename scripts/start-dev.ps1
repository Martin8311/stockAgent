[CmdletBinding()]
param(
    [switch]$SkipDocker,
    [switch]$UseH2,
    [switch]$RequireDocker,
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
$H2Dir = Join-Path $DevDir "h2"
$BackendPidFile = Join-Path $DevDir "backend.pid"
$FrontendPidFile = Join-Path $DevDir "frontend.pid"
$BackendRunScript = Join-Path $DevDir "run-backend.ps1"
$FrontendRunScript = Join-Path $DevDir "run-frontend.ps1"
$ServerPort = if ($env:SERVER_PORT) { $env:SERVER_PORT } else { "8080" }
$FrontendPort = "5173"
$script:UseLocalH2 = [bool]$UseH2
$script:RequireDockerMode = [bool]$RequireDocker

New-Item -ItemType Directory -Force -Path $DevDir, $LogDir | Out-Null
$StartupLog = Join-Path $LogDir "start-dev.log"
Start-Transcript -Path $StartupLog -Append | Out-Null

trap {
    Write-Host ""
    Write-Host "[harness-agent] Startup failed." -ForegroundColor Red
    Write-Host "[harness-agent] $($_.Exception.Message)" -ForegroundColor Red
    Write-Host "[harness-agent] Full startup log: $StartupLog"
    try {
        Stop-Transcript | Out-Null
    } catch {
    }
    exit 1
}

function Write-Step {
    param([string]$Message)
    Write-Host "[harness-agent] $Message"
}

function Repair-ProcessPathEnvironment {
    $variables = [Environment]::GetEnvironmentVariables("Process")
    $pathKeys = @()
    foreach ($key in $variables.Keys) {
        if ($key -ieq "PATH") {
            $pathKeys += $key
        }
    }

    if ($pathKeys.Count -le 1) {
        return
    }

    $preferredKey = $pathKeys | Where-Object { $_ -ceq "Path" } | Select-Object -First 1
    if ([string]::IsNullOrWhiteSpace($preferredKey)) {
        $preferredKey = $pathKeys | Select-Object -First 1
    }

    $pathValue = [Environment]::GetEnvironmentVariable($preferredKey, "Process")
    foreach ($key in $pathKeys) {
        [Environment]::SetEnvironmentVariable($key, $null, "Process")
    }
    [Environment]::SetEnvironmentVariable("Path", $pathValue, "Process")
    Write-Step "Normalized duplicate PATH variables for child process startup."
}

function New-DevSecret {
    $bytes = New-Object byte[] 32
    $generator = [System.Security.Cryptography.RandomNumberGenerator]::Create()
    try {
        $generator.GetBytes($bytes)
    } finally {
        $generator.Dispose()
    }
    return [Convert]::ToBase64String($bytes)
}

function Format-NativeOutput {
    param([object[]]$Output)

    if (!$Output -or $Output.Count -eq 0) {
        return ""
    }

    return (($Output | ForEach-Object { $_.ToString() }) -join [Environment]::NewLine).Trim()
}

function Invoke-NativeCommand {
    param(
        [string]$FilePath,
        [string[]]$Arguments,
        [switch]$Quiet
    )

    $previousErrorActionPreference = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    try {
        $output = & $FilePath @Arguments 2>&1
        $exitCode = $LASTEXITCODE
    } finally {
        $ErrorActionPreference = $previousErrorActionPreference
    }
    $text = Format-NativeOutput -Output $output

    if (!$Quiet -and ![string]::IsNullOrWhiteSpace($text)) {
        Write-Host $text
    }

    return [pscustomobject]@{
        ExitCode = $exitCode
        Output = $text
    }
}

function Enable-LocalH2Fallback {
    param(
        [string]$Reason,
        [string]$Details
    )

    $detailSuffix = ""
    if (![string]::IsNullOrWhiteSpace($Details)) {
        $detailSuffix = " Details: $Details"
    }

    if ($script:RequireDockerMode) {
        throw "$Reason$detailSuffix"
    }

    Write-Warning $Reason
    if (![string]::IsNullOrWhiteSpace($Details)) {
        Write-Warning $Details
    }
    Write-Step "Falling back to the local H2 dev profile. MySQL remains the preferred development database."
    $script:UseLocalH2 = $true
}

function Add-SpringProfile {
    param(
        [string]$Profiles,
        [string]$Profile
    )

    $items = @()
    if (![string]::IsNullOrWhiteSpace($Profiles)) {
        $items = $Profiles.Split(",") | ForEach-Object { $_.Trim() } | Where-Object { $_ }
    }
    if (!($items -contains $Profile)) {
        $items += $Profile
    }
    return ($items -join ",")
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
        [int]$TimeoutSeconds,
        [string]$PidFile = ""
    )

    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    while ((Get-Date) -lt $deadline) {
        if (![string]::IsNullOrWhiteSpace($PidFile) -and (Test-Path $PidFile) -and !(Get-TrackedProcess -PidFile $PidFile)) {
            Write-Warning "$Name process exited before it became ready. Check logs under $LogDir."
            return $false
        }

        try {
            $response = Invoke-WebRequest -Uri $Url -UseBasicParsing -TimeoutSec 3
            if ($response.StatusCode -ge 200 -and $response.StatusCode -lt 500) {
                Write-Step "$Name is ready: $Url"
                return $true
            }
        } catch {
            Start-Sleep -Seconds 2
        }
    }

    Write-Warning "$Name did not become ready within $TimeoutSeconds seconds. Check logs under $LogDir."
    return $false
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
    $escapedMavenRepo = (Join-Path $HOME ".m2\repository").Replace("'", "''")
    $backendProfileBlock = ""
    $activeProfiles = $env:SPRING_PROFILES_ACTIVE

    if ($script:UseLocalH2) {
        New-Item -ItemType Directory -Force -Path $H2Dir | Out-Null
        $h2Path = (Join-Path $H2Dir "harness_agent").Replace("\", "/")
        $h2Url = "jdbc:h2:file:$h2Path;MODE=MySQL;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE;DB_CLOSE_DELAY=-1"
        $escapedH2Url = $h2Url.Replace("'", "''")
        $activeProfiles = Add-SpringProfile -Profiles $activeProfiles -Profile "local-h2"
        Write-Step "Backend will use local H2 database files under $H2Dir."
    }

    if (![string]::IsNullOrWhiteSpace($activeProfiles)) {
        $escapedProfiles = $activeProfiles.Replace("'", "''")
        $backendProfileBlock += "`$env:SPRING_PROFILES_ACTIVE = '$escapedProfiles'`r`n"
    }

    if ($script:UseLocalH2) {
        $backendProfileBlock = @"
$backendProfileBlock
`$env:DB_URL = '$escapedH2Url'
`$env:DB_USERNAME = 'sa'
`$env:DB_PASSWORD = ''
"@
    }

    @"
`$ErrorActionPreference = "Stop"
`$env:JWT_SECRET = '$escapedSecret'
`$env:SERVER_PORT = '$ServerPort'
$backendProfileBlock
& '$escapedMaven' '-Dmaven.repo.local=$escapedMavenRepo' -f '$escapedPom' spring-boot:run
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

Repair-ProcessPathEnvironment

if (!$SkipDocker -and !$script:UseLocalH2) {
    if (!(Get-Command docker -ErrorAction SilentlyContinue)) {
        Enable-LocalH2Fallback `
            -Reason "Docker was not found. Install Docker Desktop, rerun with -SkipDocker if MySQL is already available, or use -RequireDocker to fail instead of falling back." `
            -Details ""
    } else {
        Write-Step "Checking Docker Engine access ..."
        $dockerInfo = Invoke-NativeCommand -FilePath "docker" -Arguments @("info") -Quiet
        if ($dockerInfo.ExitCode -ne 0) {
            Enable-LocalH2Fallback `
                -Reason "Docker Engine is not accessible. Start Docker Desktop, ensure the current Windows user can access Docker, or run the terminal as Administrator." `
                -Details $dockerInfo.Output
        }
    }
}

if (!$SkipDocker -and !$script:UseLocalH2) {
    Write-Step "Starting MySQL with Docker Compose ..."
    Push-Location $RootDir
    try {
        $composeResult = Invoke-NativeCommand -FilePath "docker" -Arguments @("compose", "up", "-d", "mysql")
        if ($composeResult.ExitCode -ne 0) {
            Enable-LocalH2Fallback `
                -Reason "Docker Compose could not start MySQL. You can retry with Docker fixed, use -SkipDocker with your own MySQL, or accept the local H2 fallback for demo work." `
                -Details $composeResult.Output
        }
        if (!$script:UseLocalH2 -and !$NoWait) {
            Wait-DockerHealthy -ContainerName "harness-agent-mysql" -TimeoutSeconds 90
        }
    } finally {
        Pop-Location
    }
} elseif ($SkipDocker) {
    Write-Step "Skipping Docker Compose startup. The backend will use DB_URL/DB_USERNAME/DB_PASSWORD unless -UseH2 is set."
}

if ($script:UseLocalH2) {
    Write-Step "Using local H2 dev profile. This keeps the demo runnable when Docker/MySQL is unavailable."
}

Start-Backend
Start-Frontend

if (!$NoWait) {
    $backendReady = Wait-Http -Name "Backend" -Url "http://localhost:$ServerPort/api/public/system/health" -TimeoutSeconds 120 -PidFile $BackendPidFile
    $frontendReady = Wait-Http -Name "Frontend" -Url "http://localhost:$FrontendPort" -TimeoutSeconds 60 -PidFile $FrontendPidFile
    if (!$backendReady -or !$frontendReady) {
        throw "One or more services did not become ready. Backend log: $LogDir\backend.log; frontend log: $LogDir\frontend.log"
    }
}

Write-Host ""
Write-Step "Development stack is starting."
Write-Host "Frontend:  http://localhost:$FrontendPort"
Write-Host "Backend:   http://localhost:$ServerPort"
Write-Host "Swagger:   http://localhost:$ServerPort/swagger-ui.html"
Write-Host "Logs:      $LogDir"
Write-Host "Stop:      powershell -NoProfile -ExecutionPolicy Bypass -File scripts\stop-dev.ps1"

Stop-Transcript | Out-Null
