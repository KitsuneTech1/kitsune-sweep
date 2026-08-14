function Get-SweepAdbDevices {
    param([Parameter(Mandatory)][string[]]$Lines)

    @($Lines | ForEach-Object {
        if ($_ -match '^(\S+)\s+(device|offline|unauthorized)\b') {
            [pscustomobject]@{
                Serial = $Matches[1]
                State = $Matches[2]
                IsEmulator = $Matches[1] -match '^emulator-\d+$'
            }
        }
    })
}

function Assert-NoPhysicalAndroidDevices {
    param([Parameter(Mandatory)][object[]]$Devices)

    $physical = @($Devices | Where-Object { -not $_.IsEmulator })
    if ($physical.Count -gt 0) {
        throw "A physical Android device is attached. Verification will not install tests while connected: $($physical.Serial -join ', ')"
    }
}
