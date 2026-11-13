# Daily Anitabi incremental collection + import into the site's PostgreSQL tables.
# Runs under SYSTEM via Task Scheduler. Secrets come from .env (never echoed).
$ErrorActionPreference = 'Stop'
$root = $PSScriptRoot
$logDir = Join-Path $root 'logs'
New-Item -ItemType Directory -Force -Path $logDir | Out-Null
$logFile = Join-Path $logDir ('anitabi-' + (Get-Date -Format 'yyyyMMdd') + '.log')

function Write-Log([string]$text) {
    $line = '{0} {1}' -f (Get-Date -Format 'yyyy-MM-dd HH:mm:ss'), $text
    Add-Content -Path $logFile -Value $line -Encoding UTF8
    Write-Host $line
}

# --- single instance lock -------------------------------------------------
$lockPath = Join-Path $root 'data\daily-task.lock'
$lock = $null
try {
    $lock = [System.IO.File]::Open($lockPath, 'OpenOrCreate', 'ReadWrite', 'None')
} catch {
    Write-Log 'another run holds the lock, skipping'
    exit 0
}

try {
    # --- load .env into this process -------------------------------------
    $envFile = Join-Path $root '.env'
    if (-not (Test-Path $envFile)) { Write-Log 'missing .env'; exit 2 }
    foreach ($line in Get-Content $envFile -Encoding UTF8) {
        if ($line -match '^\s*#' -or $line -notmatch '=') { continue }
        $name, $value = $line -split '=', 2
        $name = $name.Trim(); $value = $value.Trim().Trim('"').Trim("'")
        if ($name) { Set-Item -Path "Env:$name" -Value $value }
    }

    $python = Join-Path $root '.venv\Scripts\python.exe'
    $dataDir = Join-Path $root 'data'
    $idsFile = Join-Path $root 'ids.txt'

    Write-Log '--- run start ---'
    # Native stderr must not abort the wrapper on PowerShell 5.1.
    $ErrorActionPreference = 'Continue'
    Push-Location $root
    try {
        & $python 'crawler.py' --ids $idsFile --mode oss --data $dataDir --interval 3 --days 7 2>&1 |
            ForEach-Object { Write-Log ('  ' + "$_") }
        $crawlCode = $LASTEXITCODE
        Write-Log "crawler exit=$crawlCode"

        # Import whatever the export currently holds; idempotent, safe to repeat.
        & $python 'import_to_pg.py' --data $dataDir 2>&1 |
            ForEach-Object { Write-Log ('  ' + "$_") }
        $importCode = $LASTEXITCODE
        Write-Log "import exit=$importCode"
    } finally {
        Pop-Location
        $ErrorActionPreference = 'Stop'
    }

    # --- housekeeping: drop logs older than 120 days ----------------------
    Get-ChildItem $logDir -Filter 'anitabi-*.log' |
        Where-Object { $_.LastWriteTime -lt (Get-Date).AddDays(-120) } |
        Remove-Item -Force -ErrorAction SilentlyContinue

    Write-Log '--- run end ---'
    if ($crawlCode -ne 0 -or $importCode -ne 0) { exit 1 }
    exit 0
} catch {
    Write-Log ('fatal: ' + $_.Exception.Message)
    exit 3
} finally {
    if ($lock) { $lock.Dispose() }
}


