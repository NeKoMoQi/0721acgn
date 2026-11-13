# Daily CopyManga incremental download into OSS, for the works listed in comics.txt.
# Runs under SYSTEM via Task Scheduler. Secrets come from .env (never echoed).
$ErrorActionPreference = 'Stop'
$root = $PSScriptRoot
$logDir = Join-Path $root 'logs'
New-Item -ItemType Directory -Force -Path $logDir | Out-Null
$logFile = Join-Path $logDir ('copymanga-' + (Get-Date -Format 'yyyyMMdd') + '.log')

function Write-Log([string]$text) {
    $line = '{0} {1}' -f (Get-Date -Format 'yyyyMMdd HH:mm:ss'), $text
    Add-Content -Path $logFile -Value $line -Encoding UTF8
    Write-Host $line
}

$lockPath = Join-Path $root 'data\daily-task.lock'
$lock = $null
try {
    $lock = [System.IO.File]::Open($lockPath, 'OpenOrCreate', 'ReadWrite', 'None')
} catch {
    Write-Log 'another run holds the lock, skipping'
    exit 0
}

try {
    $envFile = Join-Path $root '.env'
    if (-not (Test-Path $envFile)) { Write-Log 'missing .env'; exit 2 }
    foreach ($line in Get-Content $envFile -Encoding UTF8) {
        if ($line -match '^\s*#' -or $line -notmatch '=') { continue }
        $name, $value = $line -split '=', 2
        $name = $name.Trim(); $value = $value.Trim().Trim('"').Trim("'")
        if ($name) { Set-Item -Path "Env:$name" -Value $value }
    }

    $comicsFile = Join-Path $root 'comics.txt'
    $comics = @()
    if (Test-Path $comicsFile) {
        $comics = Get-Content $comicsFile -Encoding UTF8 |
            Where-Object { $_.Trim() -and -not $_.Trim().StartsWith('#') }
    }
    if (-not $comics -or $comics.Count -eq 0) {
        Write-Log 'comics.txt has no works configured, nothing to do'
        exit 0
    }

    $base = if ($env:COPY_BASE) { $env:COPY_BASE } else { 'https://www.2026copy.com' }
    try {
        $probe = Invoke-WebRequest -Uri $base -Method Head -TimeoutSec 15 -UseBasicParsing
        Write-Log ("base reachable: {0} -> {1}" -f $base, $probe.StatusCode)
    } catch {
        Write-Log ("WAITING: {0} unreachable from this host ({1}); no run today" -f $base, $_.Exception.Message)
        exit 0
    }

    $ErrorActionPreference = 'Continue'
    Push-Location $root
    try {
        & (Join-Path $root '.venv\Scripts\python.exe') 'crawler.py' --list $comicsFile --base $base --oss --interval 4.2 2>&1 |
            ForEach-Object { Write-Log ('  ' + "$_") }
        $code = $LASTEXITCODE
        Write-Log "crawler exit=$code"
    } finally {
        Pop-Location
        $ErrorActionPreference = 'Stop'
    }

    Get-ChildItem $logDir -Filter 'copymanga-*.log' |
        Where-Object { $_.LastWriteTime -lt (Get-Date).AddDays(-120) } |
        Remove-Item -Force -ErrorAction SilentlyContinue

    if ($code -ne 0) { exit 1 }
    exit 0
} catch {
    Write-Log ('fatal: ' + $_.Exception.Message)
    exit 3
} finally {
    if ($lock) { $lock.Dispose() }
}
