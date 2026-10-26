param(
    [Parameter(Mandatory=$true)][string]$Python,
    [string]$TaskName = '0721ACGN Newsroom'
)
$ErrorActionPreference = 'Stop'
$taskPython = (Resolve-Path -LiteralPath $Python).Path
$taskRoot = $PSScriptRoot
$taskConfig = Join-Path $taskRoot 'config.json'
if (-not (Test-Path -LiteralPath $taskConfig)) { throw 'Copy config.example.json to config.json and configure it first.' }
& $taskPython (Join-Path $taskRoot 'worker.py') check --config $taskConfig
if ($LASTEXITCODE -ne 0) { throw 'Configuration check failed.' }
if (Get-ScheduledTask -TaskName $TaskName -ErrorAction SilentlyContinue) {
    throw "Task '$TaskName' already exists. Stop/unregister it explicitly before installing a replacement."
}
$taskAction = New-ScheduledTaskAction -Execute $taskPython -Argument ('"' + (Join-Path $taskRoot 'worker.py') + '" run --config "' + $taskConfig + '"') -WorkingDirectory $taskRoot
$taskTrigger = New-ScheduledTaskTrigger -AtStartup
$taskSettings = New-ScheduledTaskSettingsSet -ExecutionTimeLimit ([TimeSpan]::Zero) -RestartCount 999 -RestartInterval (New-TimeSpan -Minutes 1) -MultipleInstances IgnoreNew -StartWhenAvailable -AllowStartIfOnBatteries -DontStopIfGoingOnBatteries
# Chromium cookies use Windows user encryption. Run as the account used for browser login.
# Task Scheduler stores the credential; it is never written into a config or log.
$taskCredential = Get-Credential -UserName ([System.Security.Principal.WindowsIdentity]::GetCurrent().Name) -Message 'Use the same Windows account that completed Xiaoheihe browser login; task runs even when logged out.'
if ($null -eq $taskCredential) { throw 'Task registration cancelled.' }
$taskPassword = $taskCredential.GetNetworkCredential().Password
try {
    Register-ScheduledTask -TaskName $TaskName -Action $taskAction -Trigger $taskTrigger -Settings $taskSettings -User $taskCredential.UserName -Password $taskPassword -RunLevel Highest -Description 'Persistent ACGN platform reading and automatic article publishing' | Out-Null
} finally {
    $taskPassword = $null
    $taskCredential = $null
}
Start-ScheduledTask -TaskName $TaskName
Write-Output "Installed and started $TaskName. Check worker.py status and data/worker.log for actual health."
