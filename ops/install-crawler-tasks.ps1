# Registers the daily incremental collection tasks (SYSTEM, no logon needed).
# Re-runnable: -Force updates the existing definitions.
$ErrorActionPreference = 'Stop'

function Register-CrawlerTask {
    param(
        [string]$Name,
        [string]$Script,
        [string]$At,
        [int]$RandomDelayMinutes,
        [string]$Description
    )
    $action = New-ScheduledTaskAction -Execute 'C:\WINDOWS\System32\WindowsPowerShell\v1.0\powershell.exe' `
        -Argument ('-NoProfile -ExecutionPolicy Bypass -File "{0}"' -f $Script) `
        -WorkingDirectory (Split-Path $Script)
    $trigger = New-ScheduledTaskTrigger -Daily -At $At
    if ($RandomDelayMinutes -gt 0) {
        $trigger.RandomDelay = 'PT{0}M' -f $RandomDelayMinutes
    }
    $settings = New-ScheduledTaskSettingsSet -AllowStartIfOnBatteries -DontStopIfGoingOnBatteries `
        -StartWhenAvailable -MultipleInstances IgnoreNew `
        -ExecutionTimeLimit ([TimeSpan]::Zero) `
        -RestartCount 0
    $principal = New-ScheduledTaskPrincipal -UserId 'SYSTEM' -LogonType ServiceAccount -RunLevel Highest
    Register-ScheduledTask -TaskName $Name -Action $action -Trigger $trigger -Settings $settings `
        -Principal $principal -Description $Description -Force | Out-Null
    Write-Host ("registered: {0} daily {1} (+{2} min random)" -f $Name, $At, $RandomDelayMinutes)
}

Register-CrawlerTask -Name '0721ACGN Anitabi Crawler' `
    -Script 'C:\apps\anitabi-server\run-daily.ps1' -At '03:30' -RandomDelayMinutes 30 `
    -Description 'Anitabi 圣地巡礼增量采集 + 写入 acgn0721_gis.tb_xunli/tb_xunlipoint'

Register-CrawlerTask -Name '0721ACGN Copymanga Crawler' `
    -Script 'C:\apps\copymanga-server\run-daily.ps1' -At '04:30' -RandomDelayMinutes 30 `
    -Description '拷贝漫画增量采集（comics.txt 指定作品，图片入 OSS）'

