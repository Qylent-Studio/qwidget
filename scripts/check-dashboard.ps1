param([string]$Adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe")
$ErrorActionPreference = 'Stop'
[Console]::OutputEncoding = New-Object System.Text.UTF8Encoding($false)
$OutputEncoding = [Console]::OutputEncoding
$seen = @()
for ($page = 0; $page -lt 5; $page++) {
    & $Adb shell uiautomator dump /data/local/tmp/qwidget-dashboard-check.xml | Out-Null
    [xml]$ui = ((& $Adb shell cat /data/local/tmp/qwidget-dashboard-check.xml) -join "`n")
    $nodes = $ui.SelectNodes('//node[@package="dev.qwidget"]')
    if ($nodes.Count -eq 0) { throw 'No QWidget controls visible. Open the dashboard, unlock the device, or dismiss external system dialogs.' }
    $seen += @($nodes | ForEach-Object { $_.text })
    if ($seen -contains 'Otomasyonlar' -and ($seen | Where-Object { $_ -match '^.*zin durumu$' })) { break }
    & $Adb shell input swipe 360 1300 360 500 350
}
foreach ($required in @('Widget ekle', 'Otomasyonlar')) {
    if ($seen -notcontains $required) { throw "Dashboard section missing: $required" }
}
if (-not ($seen | Where-Object { $_ -match '^Widgetlar.m$' })) { throw 'My widgets section missing.' }
if (-not ($seen | Where-Object { $_ -match '^.*zin durumu$' })) { throw 'Permission status section missing.' }
Write-Output 'PASS: widget creation, existing widgets, automation and permission status sections are reachable.'
