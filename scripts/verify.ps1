$ErrorActionPreference = 'Stop'

$repoRoot = Split-Path -Parent $PSScriptRoot
. (Join-Path $PSScriptRoot 'VerifyHelpers.ps1')
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

function Assert-CleanReleaseTree {
    $dirty = @(& git -C $repoRoot status --porcelain=v1 --untracked-files=all)
    if ($LASTEXITCODE -ne 0) {
        throw 'Git status could not be read for the release provenance check'
    }
    if ($dirty.Count -ne 0) {
        throw "Release verification requires a clean checkout. Commit or remove: $($dirty -join '; ')"
    }
}

function Assert-ExactApkPermissions {
    param([Parameter(Mandatory)][string]$Apk)

    $aapt = Get-ChildItem (Join-Path $sdkRoot 'build-tools') -Recurse -Filter 'aapt.exe' |
        Sort-Object FullName -Descending |
        Select-Object -First 1 -ExpandProperty FullName
    if ([string]::IsNullOrWhiteSpace($aapt)) {
        throw 'Android SDK aapt.exe was not found for APK permission verification'
    }

    $actual = @(& $aapt dump permissions $Apk |
        ForEach-Object {
            if ($_ -match "uses-permission: name='([^']+)'") { $Matches[1] }
        } |
        Sort-Object -Unique)
    $expected = @(
        'android.permission.MANAGE_EXTERNAL_STORAGE',
        'android.permission.PACKAGE_USAGE_STATS',
        'android.permission.QUERY_ALL_PACKAGES'
    )
    $difference = Compare-Object -ReferenceObject $expected -DifferenceObject $actual
    if ($null -ne $difference) {
        throw "APK permission allowlist mismatch. Actual: $($actual -join ', ')"
    }
    Write-Output "Verified APK permissions: $($actual -join ', ')"
}

Push-Location $repoRoot
try {
    Assert-CleanReleaseTree
    & (Join-Path $PSScriptRoot 'test-verify-helpers.ps1')
    if ($LASTEXITCODE -ne 0) {
        throw 'ADB device safety parser tests failed.'
    }
    Invoke-SweepGradle @('testDebugUnitTest')
    Invoke-SweepGradle @('lintDebug')

    $devices = @(Get-SweepAdbDevices @(& $adb devices))
    Assert-NoPhysicalAndroidDevices $devices
    $connectedEmulators = @($devices | Where-Object { $_.IsEmulator -and $_.State -eq 'device' })
    if ($connectedEmulators.Count -gt 1) {
        throw "Verification requires one emulator, but found: $($connectedEmulators.Serial -join ', ')"
    }
    if ($connectedEmulators.Count -eq 1) {
        $serial = $connectedEmulators[0].Serial
        & $adb -s $serial shell wm size reset
        & $adb -s $serial shell cmd window user-rotation free
        & $adb -s $serial shell settings put system accelerometer_rotation 1
        & $adb -s $serial shell settings put system font_scale 1.0
        if ($LASTEXITCODE -ne 0) {
            throw "Unable to reset emulator display state on $serial"
        }
        $previousAndroidSerial = $env:ANDROID_SERIAL
        try {
            $env:ANDROID_SERIAL = $serial
            Invoke-SweepGradle @('connectedDebugAndroidTest')
        } finally {
            if ($null -eq $previousAndroidSerial) {
                Remove-Item Env:ANDROID_SERIAL -ErrorAction SilentlyContinue
            } else {
                $env:ANDROID_SERIAL = $previousAndroidSerial
            }
        }
    } else {
        Write-Output 'No connected emulator. Instrumentation tests were skipped.'
    }

    Invoke-SweepGradle @('assembleDebug')
    $apk = Join-Path $repoRoot 'app\build\outputs\apk\debug\app-debug.apk'
    if (-not (Test-Path -LiteralPath $apk)) {
        throw "Gradle did not produce the expected APK at $apk"
    }
    Assert-ExactApkPermissions $apk
    Write-Output "Verified APK: $apk"
} finally {
    Pop-Location
}
