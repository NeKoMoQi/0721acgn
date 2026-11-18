# Daily Anitabi collection: pull the site's public map dataset (www.anitabi.cn/d),
# turn it into CSV, and merge it into the site's PostgreSQL pilgrimage tables.
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
    $envFile = Join-Path $root '.env'
    if (-not (Test-Path $envFile)) { Write-Log 'missing .env'; exit 2 }
    foreach ($line in Get-Content $envFile -Encoding UTF8) {
        if ($line -match '^\s*#' -or $line -notmatch '=') { continue }
        $name, $value = $line -split '=', 2
        $name = $name.Trim(); $value = $value.Trim().Trim('"').Trim("'")
        if ($name) { Set-Item -Path "Env:$name" -Value $value }
    }

    # Console/log output must not die on Japanese names under a GBK code page.
    $env:PYTHONIOENCODING = 'utf-8'
    try { [Console]::OutputEncoding = [System.Text.Encoding]::UTF8 } catch { }
    $OutputEncoding = [System.Text.Encoding]::UTF8
    $python = Join-Path $root '.venv\Scripts\python.exe'
    $dumpDir = Join-Path $root 'data\site-dump'
    $exportDir = Join-Path $root 'data\dump-export'

    Write-Log '--- run start ---'
    $ErrorActionPreference = 'Continue'
    Push-Location $root
    try {
        & $python 'collect_dump.py' --cache $dumpDir --out $exportDir 2>&1 |
            ForEach-Object { Write-Log ('  ' + "$_") }
        $dumpCode = $LASTEXITCODE
        Write-Log "collect exit=$dumpCode"

        # Idempotent merge: new works get fresh ids, existing works are matched by
        # name, points are deduped by coordinates.
        & $python 'import_to_pg.py' --data $exportDir 2>&1 |
            ForEach-Object { Write-Log ('  ' + "$_") }
        $importCode = $LASTEXITCODE
        Write-Log "import exit=$importCode"

        # 合并 Actions 分片上传的图片映射，再把 OSS URL 换回库里。
        & $python 'merge_maps.py' 2>&1 | ForEach-Object { Write-Log ('  ' + "$_") }
        & $python 'rewrite_images.py' 2>&1 |
            ForEach-Object { Write-Log ('  ' + "$_") }
        $rewriteCode = $LASTEXITCODE
        Write-Log "rewrite exit=$rewriteCode"
    } finally {
        Pop-Location
        $ErrorActionPreference = 'Stop'
    }

    Get-ChildItem $logDir -Filter 'anitabi-*.log' |
        Where-Object { $_.LastWriteTime -lt (Get-Date).AddDays(-120) } |
        Remove-Item -Force -ErrorAction SilentlyContinue

    Write-Log '--- run end ---'
    if ($dumpCode -ne 0 -or $importCode -ne 0 -or $rewriteCode -ne 0) { exit 1 }
    exit 0
} catch {
    Write-Log ('fatal: ' + $_.Exception.Message)
    exit 3
} finally {
    if ($lock) { $lock.Dispose() }
}
