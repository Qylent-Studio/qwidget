# Verifies that the built debug APK actually carries the launcher icon: the
# AndroidManifest icon / roundIcon wiring, the adaptive-icon layers (background,
# foreground, monochrome), the density mipmap fallbacks and their presence in
# the APK zip. Read-only; no rebuild.
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
$apk = Join-Path $projectRoot 'artifacts\qwidget-studio-debug.apk'
if (-not (Test-Path $apk)) { throw "APK not found: $apk (run scripts/verify.ps1 first)" }

$sdk = if ($env:ANDROID_HOME) { $env:ANDROID_HOME } else { "$env:LOCALAPPDATA\Android\Sdk" }
# Build-tools folder names may carry suffixes (-rc1); compare only the numeric core.
$aapt2 = Get-ChildItem "$sdk\build-tools\*\aapt2.exe" -ErrorAction SilentlyContinue |
    ForEach-Object {
        if ($_.Directory.Name -match '^(\d+)\.(\d+)\.(\d+)') {
            [pscustomobject]@{ Tool = $_; Version = [version]("{0}.{1}.{2}" -f $Matches[1], $Matches[2], $Matches[3]) }
        }
    } |
    Sort-Object Version -Descending |
    Select-Object -First 1 -ExpandProperty Tool
if (-not $aapt2) { throw "aapt2 not found under $sdk\build-tools; set ANDROID_HOME" }

$failures = New-Object System.Collections.Generic.List[string]
function Assert-Match([string]$haystack, [string]$pattern, [string]$label) {
    if ($haystack -match $pattern) {
        Write-Output "OK   $label"
    } else {
        $script:failures.Add($label)
        Write-Output "FAIL $label"
    }
}

function Get-AttrRef([string]$tree, [string]$attribute) {
    $pattern = "(?m)^\s*A: http://schemas\.android\.com/apk/res/android:$attribute\(0x[0-9a-f]+\)=@(0x[0-9a-f]+)\s*$"
    if ($tree -match $pattern) { return $Matches[1] }
    return $null
}

function Resolve-Resource([string]$resources, [string]$id) {
    $pattern = "(?m)^\s*resource\s+$id\s+(\S+)"
    if ($resources -match $pattern) { return $Matches[1] }
    return $null
}

function Get-LayerRef([string]$tree, [string]$layer) {
    $pattern = "(?ms)^\s*E: $layer \(line=\d+\)\s*\r?\n\s*A: http://schemas\.android\.com/apk/res/android:drawable\(0x[0-9a-f]+\)=@(0x[0-9a-f]+)"
    if ($tree -match $pattern) { return $Matches[1] }
    return $null
}

Write-Output "APK:   $apk"
Write-Output "aapt2: $($aapt2.FullName)"

$badging = (& $aapt2.FullName dump badging $apk) -join "`n"
if ($LASTEXITCODE -ne 0) { throw 'aapt2 dump badging failed' }
$iconLines = [regex]::Matches($badging, "(?m)^application-icon-\d+:'.*'$") | ForEach-Object { $_.Value }
if ($iconLines.Count -eq 0) { $failures.Add('badging has application-icon entries') }
foreach ($line in $iconLines) {
    Assert-Match $line "res/mipmap-anydpi(-v\d+)?/ic_launcher\.xml" "manifest icon density entry -> adaptive ic_launcher ($line)"
}
Assert-Match $badging "application: label='QWidget' icon='res/mipmap-anydpi(-v\d+)?/ic_launcher\.xml'" 'manifest application icon -> adaptive ic_launcher'

$resources = (& $aapt2.FullName dump resources $apk) -join "`n"
if ($LASTEXITCODE -ne 0) { throw 'aapt2 dump resources failed' }

# Manifest wiring, resolved through the resource table.
$tree = (& $aapt2.FullName dump xmltree $apk --file AndroidManifest.xml) -join "`n"
$iconId = Get-AttrRef $tree 'icon'
$roundIconId = Get-AttrRef $tree 'roundIcon'
if (-not $iconId) { $failures.Add('manifest declares android:icon'); Write-Output 'FAIL manifest declares android:icon' }
else { Assert-Match (Resolve-Resource $resources $iconId) '^mipmap/ic_launcher$' "android:icon -> mipmap/ic_launcher (id $iconId)" }
if (-not $roundIconId) { $failures.Add('manifest declares android:roundIcon'); Write-Output 'FAIL manifest declares android:roundIcon' }
else { Assert-Match (Resolve-Resource $resources $roundIconId) '^mipmap/ic_launcher_round$' "android:roundIcon -> mipmap/ic_launcher_round (id $roundIconId)" }

foreach ($name in @(
        'drawable/ic_launcher_background',
        'drawable/ic_launcher_foreground',
        'drawable/ic_launcher_monochrome',
        'mipmap/ic_launcher',
        'mipmap/ic_launcher_round')) {
    Assert-Match $resources " $([regex]::Escape($name))\b" "resource table contains $name"
}
foreach ($density in @('mdpi', 'hdpi', 'xhdpi', 'xxhdpi', 'xxxhdpi')) {
    Assert-Match $resources "res/mipmap-$density(-v\d+)?/ic_launcher\.png" "resource table contains mipmap-$density ic_launcher.png"
}

Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [System.IO.Compression.ZipFile]::OpenRead($apk)
try {
    $entries = ($zip.Entries | ForEach-Object { $_.FullName }) -join "`n"
} finally {
    $zip.Dispose()
}
Assert-Match $entries 'res/mipmap-anydpi(-v\d+)?/ic_launcher\.xml' 'APK contains adaptive ic_launcher.xml'
Assert-Match $entries 'res/mipmap-anydpi(-v\d+)?/ic_launcher_round\.xml' 'APK contains adaptive ic_launcher_round.xml'

# Adaptive layers, resolved from the compiled adaptive-icon XML.
$adaptivePath = if ($entries -match 'res/mipmap-anydpi(-v\d+)?/ic_launcher\.xml') {
    ($entries -split "`n" | Where-Object { $_ -match '^res/mipmap-anydpi(-v\d+)?/ic_launcher\.xml$' } | Select-Object -First 1)
} else { $null }
if (-not $adaptivePath) {
    $failures.Add('adaptive ic_launcher.xml entry for layer check')
    Write-Output 'FAIL adaptive ic_launcher.xml entry for layer check'
} else {
    $adaptive = (& $aapt2.FullName dump xmltree $apk --file $adaptivePath) -join "`n"
    foreach ($layer in @('background', 'foreground', 'monochrome')) {
        $layerId = Get-LayerRef $adaptive $layer
        $resolved = if ($layerId) { Resolve-Resource $resources $layerId } else { $null }
        Assert-Match ([string]$resolved) "^drawable/ic_launcher_$layer$" "adaptive <$layer> -> drawable/ic_launcher_$layer"
    }
}

foreach ($density in @('mdpi', 'hdpi', 'xhdpi', 'xxhdpi', 'xxxhdpi')) {
    foreach ($file in @('ic_launcher.png', 'ic_launcher_round.png')) {
        $escaped = [regex]::Escape($file)
        Assert-Match $entries "res/mipmap-$density(-v\d+)?/$escaped" "APK contains mipmap-$density/$file"
    }
}

if ($failures.Count -gt 0) {
    throw "Icon resource check failed: $($failures.Count) problem(s)"
}
Write-Output 'Icon resource check passed.'