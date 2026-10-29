param([string]$Adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe")
$ErrorActionPreference = 'Stop'
[Console]::OutputEncoding = New-Object System.Text.UTF8Encoding($false)
$OutputEncoding = [Console]::OutputEncoding
& $Adb shell uiautomator dump /data/local/tmp/qwidget-check.xml | Out-Null
[xml]$ui = ((& $Adb shell cat /data/local/tmp/qwidget-check.xml) -join "`n")
$nodes = $ui.SelectNodes('//node[@package="dev.qwidget"]')
$configurationTitle = "Rengini se$([char]0x00e7)"
$permissionTitle = "Bildirim eri$([char]0x015f)imi"
if (-not ($nodes | Where-Object { $_.text -eq $configurationTitle })) { throw 'Open an actual widget configuration screen first.' }
if (-not ($nodes | Where-Object { $_.text -eq $permissionTitle })) { throw 'FAIL: configuration has no permission recovery button.' }
$spotifyTitle = "Spotify'$([char]0x0131) a$([char]0x00e7)"
if (-not ($nodes | Where-Object { $_.text -eq $spotifyTitle })) { throw 'FAIL: configuration has no Spotify recovery button.' }
Write-Output 'PASS: widget configuration provides permission and Spotify recovery actions.'
