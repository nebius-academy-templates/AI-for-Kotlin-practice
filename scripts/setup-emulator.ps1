[CmdletBinding()]
param(
    [string]$AvdName = "",
    [ValidateRange(1536, 4096)]
    [int]$RamMb = 2048,
    [switch]$CheckOnly,
    [switch]$NoStart
)

$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

function Resolve-SdkRoot {
    foreach ($candidate in @(
        $env:ANDROID_HOME,
        $env:ANDROID_SDK_ROOT,
        (Join-Path $env:LOCALAPPDATA "Android\Sdk")
    )) {
        if ($candidate -and (Test-Path -LiteralPath $candidate)) {
            return (Resolve-Path -LiteralPath $candidate).Path
        }
    }
    throw "Android SDK not found. Install Android Studio, then set ANDROID_HOME."
}

function Find-SdkTool([string]$SdkRoot, [string]$Tool) {
    $candidates = @(
        (Join-Path $SdkRoot "cmdline-tools\latest\bin\$Tool.bat"),
        (Join-Path $SdkRoot "tools\bin\$Tool.bat")
    )
    $versioned = Get-ChildItem -LiteralPath (Join-Path $SdkRoot "cmdline-tools") -Directory -ErrorAction SilentlyContinue |
        Sort-Object Name -Descending |
        ForEach-Object { Join-Path $_.FullName "bin\$Tool.bat" }
    foreach ($candidate in @($candidates + $versioned)) {
        if (Test-Path -LiteralPath $candidate) {
            return $candidate
        }
    }
    throw "$Tool was not found. In Android Studio install Android SDK Command-line Tools (latest)."
}

function Read-IniValue([string]$Path, [string]$Key) {
    $line = Get-Content -LiteralPath $Path | Where-Object { $_ -match "^$([regex]::Escape($Key))\s*=" } | Select-Object -Last 1
    if ($line) {
        return ($line -split "=", 2)[1].Trim()
    }
    return $null
}

function Set-IniValue([string]$Path, [string]$Key, [string]$Value) {
    $lines = @(Get-Content -LiteralPath $Path)
    $updated = $false
    $newLines = foreach ($line in $lines) {
        if ($line -match "^$([regex]::Escape($Key))\s*=") {
            if (-not $updated) {
                "$Key = $Value"
                $updated = $true
            }
        } else {
            $line
        }
    }
    if (-not $updated) {
        $newLines += "$Key = $Value"
    }
    Set-Content -LiteralPath $Path -Value $newLines -Encoding ascii
}

function Find-RunningAvdSerial([string]$Adb, [string]$Name) {
    $serials = & $Adb devices |
        Select-String '^emulator-\d+\s+device$' |
        ForEach-Object { ($_.Line -split '\s+')[0] }
    foreach ($serial in $serials) {
        $runningName = (& $Adb -s $serial emu avd name 2>$null | Select-Object -First 1).Trim()
        if ($runningName -eq $Name) {
            return $serial
        }
    }
    return $null
}

function Assert-FreeSpace([string]$Path, [long]$RequiredBytes, [string]$Purpose) {
    $existing = $Path
    while (-not (Test-Path -LiteralPath $existing)) {
        $parent = Split-Path -Parent $existing
        if (-not $parent -or $parent -eq $existing) {
            throw "Cannot resolve a disk for $Path."
        }
        $existing = $parent
    }
    $root = (Get-Item -LiteralPath $existing).PSDrive.Root
    $free = [IO.DriveInfo]::new($root).AvailableFreeSpace
    if ($free -lt $RequiredBytes) {
        $freeGb = [math]::Round($free / 1GB, 1)
        $requiredGb = [math]::Round($RequiredBytes / 1GB, 1)
        throw "$Purpose needs at least $requiredGb GB free on $root; only $freeGb GB is available."
    }
}

$sdkRoot = Resolve-SdkRoot
$emulator = Join-Path $sdkRoot "emulator\emulator.exe"
$adb = Join-Path $sdkRoot "platform-tools\adb.exe"
$sdkManager = Find-SdkTool $sdkRoot "sdkmanager"
$avdManager = Find-SdkTool $sdkRoot "avdmanager"

$abi = if ($env:PROCESSOR_ARCHITECTURE -eq "ARM64") { "arm64-v8a" } else { "x86_64" }
if (-not $AvdName) {
    $AvdName = "Pixel_6"
}
$image = "system-images;android-36;google_apis;$abi"
$avdHome = if ($env:ANDROID_AVD_HOME) { $env:ANDROID_AVD_HOME } else { Join-Path $env:USERPROFILE ".android\avd" }
$config = Join-Path $avdHome "$AvdName.avd\config.ini"

Write-Host "AI QA Sandbox emulator setup"
Write-Host "SDK: $sdkRoot"
Write-Host "Host architecture: $env:PROCESSOR_ARCHITECTURE"
Write-Host "AVD: $AvdName"
Write-Host "Image: $image"
Write-Host "RAM: $RamMb MB"

if ($CheckOnly) {
    if (-not (Test-Path -LiteralPath $emulator) -or -not (Test-Path -LiteralPath $adb)) {
        throw "Android Emulator or Platform-Tools are missing. Run this script without -CheckOnly."
    }
    if (-not (Test-Path -LiteralPath $config)) {
        throw "AVD $AvdName does not exist. Run this script without -CheckOnly."
    }
    $actualAbi = Read-IniValue $config "abi.type"
    $actualImage = Read-IniValue $config "image.sysdir.1"
    $actualRam = Read-IniValue $config "hw.ramSize"
    if ($actualAbi -ne $abi) {
        throw "AVD ABI is $actualAbi; expected $abi for this host."
    }
    if ($actualImage -notmatch "android-36.*google_apis.*$([regex]::Escape($abi))") {
        throw "AVD image is $actualImage; expected API 36 google_apis $abi."
    }
    $ramNumber = [int]($actualRam -replace '[^0-9]', '')
    if ($actualRam -match 'G$') {
        $ramNumber *= 1024
    }
    if ($ramNumber -lt 1536 -or $ramNumber -gt 4096) {
        throw "AVD RAM is $actualRam; expected 1536 to 4096 MB."
    }
    Write-Host "[OK] $AvdName uses API 36, $abi and $actualRam RAM."
    exit 0
}

$requiredPackages = @("platform-tools", "emulator", "platforms;android-36", "build-tools;36.0.0", $image)
$savedErrorPreference = $ErrorActionPreference
$ErrorActionPreference = "Continue"
$installedLines = & $sdkManager --list_installed 2>&1
$sdkListExit = $LASTEXITCODE
$ErrorActionPreference = $savedErrorPreference
if ($sdkListExit -ne 0) {
    throw "sdkmanager could not list installed packages. Review its output above."
}
$installedOutput = $installedLines -join "`n"
$missingPackages = @($requiredPackages | Where-Object {
    $installedOutput -notmatch "(?m)^\s*$([regex]::Escape($_))\s*\|"
})
if ($missingPackages.Count -gt 0) {
    Assert-FreeSpace $sdkRoot 8GB "Installing Android SDK packages"
    Write-Host "Installing missing Android SDK packages: $($missingPackages -join ', ')"
    $ErrorActionPreference = "Continue"
    & $sdkManager @missingPackages
    $sdkInstallExit = $LASTEXITCODE
    $ErrorActionPreference = $savedErrorPreference
    if ($sdkInstallExit -ne 0) {
        throw "sdkmanager failed. Review its error above; common causes are unaccepted licenses, insufficient disk space or a network failure."
    }
} else {
    Write-Host "[OK] Required Android SDK packages are already installed."
}
if (-not (Test-Path -LiteralPath $emulator) -or -not (Test-Path -LiteralPath $adb)) {
    throw "sdkmanager finished but Emulator or Platform-Tools are still missing in $sdkRoot."
}

$existingAvds = @(& $emulator -list-avds)
if ($AvdName -notin $existingAvds) {
    Assert-FreeSpace $avdHome 8GB "Creating the practice AVD"
    Write-Host "Creating AVD $AvdName"
    $ErrorActionPreference = "Continue"
    "no" | & $avdManager create avd --name $AvdName --package $image --device "pixel_6"
    $avdCreateExit = $LASTEXITCODE
    $ErrorActionPreference = $savedErrorPreference
    if ($avdCreateExit -ne 0) {
        throw "avdmanager could not create $AvdName."
    }
} else {
    Write-Host "Reusing existing AVD $AvdName"
}

if (-not (Test-Path -LiteralPath $config)) {
    throw "AVD config was not created at $config."
}
$actualAbi = Read-IniValue $config "abi.type"
$actualImage = Read-IniValue $config "image.sysdir.1"
if ($actualAbi -ne $abi -or $actualImage -notmatch "android-36.*google_apis.*$([regex]::Escape($abi))") {
    throw "Existing AVD $AvdName uses '$actualImage' / '$actualAbi'. Delete it in Device Manager, then rerun this script."
}
Set-IniValue $config "hw.ramSize" "$RamMb"
Write-Host "[OK] AVD $AvdName is ready with $RamMb MB RAM."

if ($NoStart) {
    Write-Host "Start it later in Android Studio Device Manager or rerun without -NoStart."
    exit 0
}

& $emulator -accel-check
if ($LASTEXITCODE -ne 0) {
    throw "Android Emulator hardware acceleration is unavailable. Enable virtualization and retry."
}

$serial = Find-RunningAvdSerial $adb $AvdName
if (-not $serial) {
    $logDir = Join-Path (Resolve-Path (Join-Path $PSScriptRoot "..")).Path "build\verification"
    New-Item -ItemType Directory -Force -Path $logDir | Out-Null
    Write-Host "Starting $AvdName in a visible emulator window."
    Start-Process -FilePath $emulator -ArgumentList @("-avd", $AvdName) -RedirectStandardOutput (Join-Path $logDir "$AvdName.out.log") -RedirectStandardError (Join-Path $logDir "$AvdName.err.log") | Out-Null
}

$deadline = (Get-Date).AddMinutes(3)
while ((Get-Date) -lt $deadline) {
    $serial = Find-RunningAvdSerial $adb $AvdName
    if ($serial -and ((& $adb -s $serial shell getprop sys.boot_completed).Trim() -eq "1")) {
        foreach ($setting in "window_animation_scale", "transition_animation_scale", "animator_duration_scale") {
            & $adb -s $serial shell settings put global $setting 0 | Out-Null
        }
        Write-Host "[OK] $AvdName booted as $serial; animations are disabled."
        Write-Host "Next: .\scripts\bootstrap.ps1"
        exit 0
    }
    Start-Sleep -Seconds 2
}

throw "AVD $AvdName did not finish booting within 3 minutes. Check build\verification\$AvdName.err.log."
