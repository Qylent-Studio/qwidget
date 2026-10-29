$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
if (-not $env:JAVA_HOME) {
    $studioJbr = 'C:\Program Files\Android\Android Studio\jbr'
    if (Test-Path $studioJbr) { $env:JAVA_HOME = $studioJbr }
}
if (-not $env:ANDROID_HOME) { $env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk" }
if (-not $env:QWIDGET_BUILD_ROOT) {
    $env:QWIDGET_BUILD_ROOT = "$env:LOCALAPPDATA\Temp\opencode\qwidget-build"
}
Push-Location $projectRoot
try {
    & .\gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:lintDebug --console=plain
    if ($LASTEXITCODE -ne 0) { throw "Verification failed: $LASTEXITCODE" }
    New-Item -ItemType Directory -Force artifacts | Out-Null
    Copy-Item "$env:QWIDGET_BUILD_ROOT\app\outputs\apk\debug\app-debug.apk" 'artifacts\qwidget-probe-debug.apk' -Force
    Copy-Item "$env:QWIDGET_BUILD_ROOT\app\outputs\apk\debug\app-debug.apk" 'artifacts\qwidget-widget-debug.apk' -Force
    Copy-Item "$env:QWIDGET_BUILD_ROOT\app\outputs\apk\debug\app-debug.apk" 'artifacts\qwidget-automation-debug.apk' -Force
    Copy-Item "$env:QWIDGET_BUILD_ROOT\app\outputs\apk\debug\app-debug.apk" 'artifacts\qwidget-battery-debug.apk' -Force
    Copy-Item "$env:QWIDGET_BUILD_ROOT\app\outputs\apk\debug\app-debug.apk" 'artifacts\qwidget-studio-debug.apk' -Force
    Write-Output 'APK: artifacts\qwidget-studio-debug.apk (older filenames retained for compatibility)'
} finally { Pop-Location }
