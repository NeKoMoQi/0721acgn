<#
.SYNOPSIS
    检测 GitHub 上 frontend/ 的更新，自动拉取、构建并替换 dist。

.DESCRIPTION
    给 0721ACGN 用的前端自动发布脚本，适合放进计划任务每分钟/每几分钟跑一次：

      1. git fetch 远端，比较「上次成功发布时的提交」和 origin/main
      2. 只有 frontend/ 下有变化才继续（后端/采集数据变化会记日志并提示手动部署）
      3. 工作区不干净或有未推送提交时直接跳过，绝不 reset / 覆盖本地改动
      4. fast-forward 拉取 -> 备份 dist -> npm run build-only -> 成功才更新状态
      5. 构建失败自动还原旧 dist，站点不会空掉

    状态、日志和锁文件都放在 <repo>\.deploy\ 下（已加入 .gitignore）。

.PARAMETER Branch
    跟踪的分支，默认 main。

.PARAMETER DryRun
    只检查并打印「会做什么」，不拉代码、不构建、不写状态。

.PARAMETER Force
    即使提交没有变化也强制重新构建一次。

.EXAMPLE
    powershell -NoProfile -ExecutionPolicy Bypass -File scripts\auto-deploy-frontend.ps1

.EXAMPLE
    powershell -NoProfile -ExecutionPolicy Bypass -File scripts\auto-deploy-frontend.ps1 -DryRun
#>
[CmdletBinding()]
param(
    [string]$Branch = 'main',
    [switch]$DryRun,
    [switch]$Force
)

$ErrorActionPreference = 'Stop'

$RepoRoot = Split-Path -Parent $PSScriptRoot
$StateDir = Join-Path $RepoRoot '.deploy'
$StateFile = Join-Path $StateDir 'frontend-state.json'
$LogFile = Join-Path $StateDir 'deploy.log'
$LockFile = Join-Path $StateDir 'deploy.lock'
$FrontendDir = Join-Path $RepoRoot 'frontend'
$DistDir = Join-Path $FrontendDir 'dist'
$NodeBin = 'C:\tools\nodejs'

if (-not (Test-Path $StateDir)) { New-Item -ItemType Directory -Path $StateDir -Force | Out-Null }

function Write-Log {
    param([string]$Message)
    $line = '{0} {1}' -f (Get-Date -Format 'yyyy-MM-dd HH:mm:ss'), $Message
    Write-Output $line
    Add-Content -Path $LogFile -Value $line -Encoding UTF8
    # 日志过大时轮转，只留一份 .1
    if ((Get-Item $LogFile).Length -gt 2MB) {
        Move-Item -Path $LogFile -Destination ($LogFile + '.1') -Force
    }
}

function Resolve-Git {
    $git = (Get-Command git -ErrorAction SilentlyContinue).Source
    if (-not $git) {
        $candidates = @(
            (Join-Path $env:LOCALAPPDATA 'Programs\MinGit\cmd\git.exe'),
            'C:\Program Files\Git\cmd\git.exe'
        )
        foreach ($c in $candidates) { if (Test-Path $c) { $git = $c; break } }
    }
    if (-not $git) { throw '找不到 git.exe' }
    return $git
}

function Invoke-Git {
    param([string[]]$Arguments, [switch]$AllowFail)
    $out = & $Git -C $RepoRoot @Arguments 2>&1
    $code = $LASTEXITCODE
    if ($code -ne 0 -and -not $AllowFail) {
        throw ("git {0} 失败({1}): {2}" -f ($Arguments -join ' '), $code, ($out -join ' '))
    }
    return @{ Code = $code; Output = $out }
}

# Windows PowerShell 5.1 会把原生命令的 stderr 当成错误记录，配合 ErrorActionPreference=Stop 会中断脚本。
# 统一交给 cmd 做重定向，只取退出码；输出全部写日志。
function Invoke-Npm {
    param([string]$Arguments)
    $line = '"{0}" {1} >> "{2}" 2>&1' -f $npm, $Arguments, $LogFile
    & cmd.exe /c $line | Out-Null
    return $LASTEXITCODE
}

function Read-State {
    if (Test-Path $StateFile) {
        try { return Get-Content $StateFile -Raw -Encoding UTF8 | ConvertFrom-Json } catch { }
    }
    return $null
}

function Write-State {
    param([string]$DeployedCommit, [string]$Result)
    $now = (Get-Date).ToString('o')
    $state = [ordered]@{
        lastDeployedCommit = $DeployedCommit
        lastDeployedAt     = $now
        lastResult         = $Result
        repo               = $RepoRoot
    }
    $state | ConvertTo-Json | Set-Content -Path $StateFile -Encoding UTF8
}

function Remove-OldBackups {
    Get-ChildItem -Path $FrontendDir -Directory -Filter 'dist-backup-*' -ErrorAction SilentlyContinue |
        Sort-Object LastWriteTime -Descending |
        Select-Object -Skip 3 |
        ForEach-Object { Remove-Item $_.FullName -Recurse -Force -ErrorAction SilentlyContinue }
}

# ---------- 单实例锁 ----------
if (Test-Path $LockFile) {
    $lockPid = (Get-Content $LockFile -ErrorAction SilentlyContinue | Select-Object -First 1)
    $alive = $null
    if ($lockPid -and ($lockPid -as [int])) {
        $alive = Get-Process -Id ([int]$lockPid) -ErrorAction SilentlyContinue
    }
    if ($alive) {
        Write-Log "上一次运行还在进行中 (pid $lockPid)，本次跳过"
        exit 0
    }
    Remove-Item $LockFile -Force -ErrorAction SilentlyContinue
}
Set-Content -Path $LockFile -Value $PID -Encoding ASCII

try {
    $Git = Resolve-Git
    $env:GIT_SSH_COMMAND = 'C:/Windows/System32/OpenSSH/ssh.exe'
    $env:Path = "$NodeBin;$env:Path"
    $npm = Join-Path $NodeBin 'npm.cmd'
    if (-not (Test-Path $npm)) { throw "找不到 $npm" }

    Remove-OldBackups

    # ---------- 远端状态 ----------
    Invoke-Git @('fetch', '--quiet', 'origin', $Branch) | Out-Null
    $remote = (Invoke-Git @('rev-parse', "origin/$Branch")).Output.Trim()
    $head = (Invoke-Git @('rev-parse', 'HEAD')).Output.Trim()
    $state = Read-State

    if (-not $state) {
        if ($DryRun) {
            Write-Log "[dry-run] 没有发布状态文件，首次运行会把当前 HEAD ($($head.Substring(0,8))) 记为已发布"
        } else {
            Write-State -DeployedCommit $head -Result 'bootstrap'
            Write-Log "首次运行：把当前 HEAD $($head.Substring(0,8)) 记为已发布基线（dist 视为与之一致）"
        }
        exit 0
    }

    $lastDeployed = $state.lastDeployedCommit
    if (-not $lastDeployed) { $lastDeployed = $head }

    if ($remote -eq $lastDeployed -and -not $Force) {
        Write-Log "已是最新：$($remote.Substring(0,8))，无需操作"
        exit 0
    }

    # ---------- 变了哪些文件 ----------
    $diffArgs = @('diff', '--name-only', $lastDeployed, $remote)
    $changed = (Invoke-Git $diffArgs).Output | Where-Object { $_ -and $_.Trim() }
    $frontendChanged = @($changed | Where-Object { $_ -like 'frontend/*' })
    $backendChanged = @($changed | Where-Object { $_ -like 'backend/*' -or $_ -like 'newsroom/*' })

    if ($backendChanged.Count -gt 0) {
        Write-Log ("注意：有 $($backendChanged.Count) 个后端/newsroom 文件变化，本脚本只发前端，需要手动打包重启 API")
    }

    if ($frontendChanged.Count -eq 0 -and -not $Force) {
        if ($DryRun) {
            Write-Log "[dry-run] $($lastDeployed.Substring(0,8)) -> $($remote.Substring(0,8)) 没有 frontend/ 变化，跳过"
        } else {
            Write-State -DeployedCommit $remote -Result 'no-frontend-change'
            Write-Log "$($lastDeployed.Substring(0,8)) -> $($remote.Substring(0,8)) 没有 frontend/ 变化，仅推进记录"
        }
        exit 0
    }

    Write-Log ("检测到前端更新 {0} -> {1}，{2} 个 frontend 文件" -f $lastDeployed.Substring(0,8), $remote.Substring(0,8), $frontendChanged.Count)
    $frontendChanged | Select-Object -First 30 | ForEach-Object { Write-Log "  · $_" }
    if ($frontendChanged.Count -gt 30) { Write-Log "  ... 以及另外 $($frontendChanged.Count - 30) 个" }

    if ($DryRun) {
        Write-Log "[dry-run] 会执行：git merge --ff-only origin/$Branch -> 备份 dist -> npm run build-only -> 更新状态"
        exit 0
    }

    # ---------- 安全检查 ----------
    $dirty = (Invoke-Git @('status', '--porcelain')).Output | Where-Object { $_ -and $_.Trim() }
    if ($dirty) {
        Write-Log "工作区有未提交改动，跳过本次自动发布（避免覆盖本地修改）："
        $dirty | Select-Object -First 10 | ForEach-Object { Write-Log "  · $_" }
        exit 1
    }

    if ($head -ne $remote) {
        $ff = Invoke-Git @('merge', '--ff-only', "origin/$Branch") -AllowFail
        if ($ff.Code -ne 0) {
            Write-Log "无法 fast-forward（本地有未推送提交？），跳过：$(($ff.Output | Select-Object -First 3) -join ' ')"
            exit 1
        }
        Write-Log "已 fast-forward 到 $($remote.Substring(0,8))"
    }

    # 锁文件变化时补依赖
    $lockChanged = $changed | Where-Object { $_ -eq 'frontend/package-lock.json' }
    if ($lockChanged) {
        Write-Log "package-lock.json 有变化，先 npm install"
        Push-Location $FrontendDir
        try {
            $installExit = Invoke-Npm 'install --no-audit --no-fund'
            if ($installExit -ne 0) { throw "npm install 失败($installExit)" }
        } finally { Pop-Location }
    }

    # ---------- 构建（失败可回滚） ----------
    $stamp = Get-Date -Format 'yyyyMMdd-HHmmss'
    $backup = Join-Path $FrontendDir "dist-backup-$stamp"
    $hadDist = Test-Path $DistDir
    $script:backup = $backup
    $script:hadDist = $hadDist
    if ($hadDist) {
        Copy-Item $DistDir $backup -Recurse -Force
        Write-Log "已备份旧 dist -> $(Split-Path $backup -Leaf)"
    }

    Write-Log "开始构建：npm run build-only"
    $buildStart = Get-Date
    Push-Location $FrontendDir
    try {
        $buildExit = Invoke-Npm 'run build-only'
    } finally { Pop-Location }

    if ($buildExit -ne 0) {
        Write-Log "构建失败(exit $buildExit)，还原旧 dist"
        if ($hadDist -and (Test-Path $backup)) {
            if (Test-Path $DistDir) { Remove-Item $DistDir -Recurse -Force }
            Move-Item $backup $DistDir
        }
        exit 1
    }

    if (-not (Test-Path (Join-Path $DistDir 'index.html'))) {
        Write-Log "构建结束但没有 dist/index.html，判定失败并还原"
        if ($hadDist -and (Test-Path $backup)) {
            if (Test-Path $DistDir) { Remove-Item $DistDir -Recurse -Force }
            Move-Item $backup $DistDir
        }
        exit 1
    }

    $cost = [int]((Get-Date) - $buildStart).TotalSeconds
    Write-State -DeployedCommit $remote -Result 'success'
    Remove-OldBackups
    Write-Log ("发布完成：{0}，耗时 {1}s，dist/index.html 已更新" -f $remote.Substring(0,8), $cost)
    exit 0
}
catch {
    Write-Log "运行异常：$($_.Exception.Message)"
    # 构建过程中出错时，只要 dist 已经不完整就还原备份，别把站点留成空壳
    if ($script:hadDist -and $script:backup -and (Test-Path $script:backup) -and
        -not (Test-Path (Join-Path $DistDir 'index.html'))) {
        if (Test-Path $DistDir) { Remove-Item $DistDir -Recurse -Force }
        Move-Item $script:backup $DistDir
        Write-Log "已还原旧 dist"
    }
    exit 1
}
finally {
    Remove-Item $LockFile -Force -ErrorAction SilentlyContinue
}
