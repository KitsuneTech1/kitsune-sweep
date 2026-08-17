$ErrorActionPreference = 'Stop'

. (Join-Path $PSScriptRoot 'VerifyHelpers.ps1')

$emulatorOnly = @(Get-SweepAdbDevices @(
    'List of devices attached',
    'emulator-5554 device product:sdk_gphone model:sdk_gphone transport_id:1',
    ''
))
if ($emulatorOnly.Count -ne 1 -or -not $emulatorOnly[0].IsEmulator) {
    throw 'The adb parser did not recognize one emulator.'
}
Assert-NoPhysicalAndroidDevices $emulatorOnly

$noDevices = @(Get-SweepAdbDevices @('List of devices attached', ''))
Assert-NoPhysicalAndroidDevices $noDevices

$mixed = @(Get-SweepAdbDevices @(
    'List of devices attached',
    'emulator-5554 device product:sdk_gphone',
    'R3CW1234ABC device product:e3q model:SM_S928U'
))
$mixedRejected = $false
try {
    Assert-NoPhysicalAndroidDevices $mixed
} catch {
    $mixedRejected = $true
}
if (-not $mixedRejected) {
    throw 'A mixed emulator and physical-device list was not rejected.'
}

$unauthorizedPhone = @(Get-SweepAdbDevices @('R3CW1234ABC unauthorized usb:1-1'))
$unauthorizedRejected = $false
try {
    Assert-NoPhysicalAndroidDevices $unauthorizedPhone
} catch {
    $unauthorizedRejected = $true
}
if (-not $unauthorizedRejected) {
    throw 'An unauthorized physical device was not rejected.'
}

Write-Output 'ADB device safety parser passed.'
