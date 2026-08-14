$ErrorActionPreference = 'Stop'

$repoRoot = Split-Path -Parent $PSScriptRoot
$gradle = Join-Path $repoRoot 'gradlew.bat'
$androidJbr = 'C:\Program Files\Android\Android Studio\jbr'
if (Test-Path -LiteralPath $androidJbr) {
    $env:JAVA_HOME = $androidJbr
}

$sdkRoot = $env:ANDROID_SDK_ROOT
if ([string]::IsNullOrWhiteSpace($sdkRoot)) {
    $sdkRoot = Join-Path $env:LOCALAPPDATA 'Android\Sdk'
}
$apiPlatform = Join-Path $sdkRoot 'platforms\android-36'
if (-not (Test-Path -LiteralPath $apiPlatform)) {
    throw "Android SDK platform 36 is not installed at $apiPlatform"
}
$adb = Join-Path $sdkRoot 'platform-tools\adb.exe'
if (-not (Test-Path -LiteralPath $adb)) {
    throw "Android SDK adb was not found at $adb"
}

function Invoke-SweepGradle {
    param([Parameter(Mandatory)][string[]]$Tasks)

    & $gradle @Tasks '--no-daemon' '--max-workers=1'
    if ($LASTEXITCODE -ne 0) {
        throw "Gradle failed while running: $($Tasks -join ' ')"
    }
}

Push-Location $repoRoot
try {
    Invoke-SweepGradle @('testDebugUnitTest')
    Invoke-SweepGradle @('lintDebug')

    $connectedEmulator = & $adb devices | Select-String -Pattern '^emulator-\d+\s+device$'
    if ($null -ne $connectedEmulator) {
        $serial = ($connectedEmulator -split '\s+')[0]
        & $adb -s $serial shell wm size reset
        & $adb -s $serial shell cmd window user-rotation free
        & $adb -s $serial shell settings put system accelerometer_rotation 1
        & $adb -s $serial shell settings put system font_scale 1.0
        if ($LASTEXITCODE -ne 0) {
            throw "Unable to reset emulator display state on $serial"
        }
        Invoke-SweepGradle @('connectedDebugAndroidTest')
    } else {
        Write-Output 'No connected emulator. Instrumentation tests were skipped.'
    }

    Invoke-SweepGradle @('assembleDebug')
    $apk = Join-Path $repoRoot 'app\build\outputs\apk\debug\app-debug.apk'
    if (-not (Test-Path -LiteralPath $apk)) {
        throw "Gradle did not produce the expected APK at $apk"
    }
    Write-Output "Verified APK: $apk"
} finally {
    Pop-Location
}
