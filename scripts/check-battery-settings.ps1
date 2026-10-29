param([string]$Adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe")
$ErrorActionPreference = 'Stop'
[Console]::OutputEncoding = New-Object System.Text.UTF8Encoding($false)
$OutputEncoding = [Console]::OutputEncoding
& $Adb shell uiautomator dump /data/local/tmp/qwidget-battery-check.xml | Out-Null
[xml]$ui = ((& $Adb shell cat /data/local/tmp/qwidget-battery-check.xml) -join "`n")
$nodes = $ui.SelectNodes('//node[@package="dev.qwidget"]')
if (-not ($nodes | Where-Object { $_.text -match '^Rengini se' })) { throw 'Open battery widget settings by tapping the widget first.' }
if ($nodes | Where-Object { $_.text -match '^Spotify|^Bildirim' }) { throw 'Battery configuration must not require Spotify access.' }
if (-not ($nodes | Where-Object { $_.text -match '^%\d+$|^—$' })) { throw 'Battery preview missing.' }
if (@($nodes | Where-Object { $_.class -eq 'android.widget.RadioButton' -and $_.checked -eq 'true' }).Count -ne 1) {
    throw 'Exactly one palette should be selected.'
}
Write-Output 'PASS: battery configuration has percentage preview, palette selection, and no Spotify access controls.'
