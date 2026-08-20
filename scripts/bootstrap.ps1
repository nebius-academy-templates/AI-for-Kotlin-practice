[CmdletBinding()]
param()

$ErrorActionPreference = "Continue"
Set-StrictMode -Version Latest
$workspace = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
Set-Location -LiteralPath $workspace
Remove-Item Env:APPIUM_HOME -ErrorAction SilentlyContinue

$passed = 0
$failed = 0

function Test-Check([string]$Name, [scriptblock]$Check, [string]$Fix) {
    try {
        if (& $Check) {
            Write-Host "[OK]   $Name"
            $script:passed++
            return
        }
    } catch {
        # The actionable fix below is more useful than a PowerShell stack trace.
    }
    Write-Host "[FAIL] $Name"
    Write-Host "       $Fix"
    $script:failed++
}

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
    return $null
}

function Get-AvdConfig([string]$Name) {
    $avdHome = if ($env:ANDROID_AVD_HOME) { $env:ANDROID_AVD_HOME } else { Join-Path $env:USERPROFILE ".android\avd" }
    return Join-Path $avdHome "$Name.avd\config.ini"
}

Write-Host "AI QA Sandbox environment doctor"
$sdkRoot = Resolve-SdkRoot
$abi = if ($env:PROCESSOR_ARCHITECTURE -eq "ARM64") { "arm64-v8a" } else { "x86_64" }
$avdName = "Pixel_6"
$config = Get-AvdConfig $avdName

Test-Check "JDK 17" {
    $version = (& java -version 2>&1 | Select-Object -First 1) -join ""
    return $version -match 'version "17(\.|\")'
} "Install JDK 17 and point JAVA_HOME at it."

Test-Check "Node.js 20+" {
    $major = [int]((& node --version) -replace '^v([0-9]+).*$', '$1')
    return $major -ge 20
} "Install Node.js 20 LTS or newer."

Test-Check "Android SDK root" {
    return $null -ne $sdkRoot
} "Install Android Studio and set ANDROID_HOME to its Android SDK directory."

Test-Check "Android platform-tools (adb)" {
    return $sdkRoot -and (Test-Path -LiteralPath (Join-Path $sdkRoot "platform-tools\adb.exe"))
} "Install Android SDK Platform-Tools in Android Studio SDK Manager."

Test-Check "Android Emulator" {
    return $sdkRoot -and (Test-Path -LiteralPath (Join-Path $sdkRoot "emulator\emulator.exe"))
} "Install Android Emulator in Android Studio SDK Manager."

Test-Check "Emulator hardware acceleration" {
    if (-not $sdkRoot) {
        return $false
    }
    $emulator = Join-Path $sdkRoot "emulator\emulator.exe"
    if (-not (Test-Path -LiteralPath $emulator)) {
        return $false
    }
    & $emulator -accel-check | Out-Null
    return $LASTEXITCODE -eq 0
} "Enable CPU virtualization; on Linux also configure KVM access."

Test-Check "Android API 36 platform" {
    return $sdkRoot -and (Test-Path -LiteralPath (Join-Path $sdkRoot "platforms\android-36\android.jar"))
} "Install Android SDK Platform 36."

Test-Check "Android build-tools 36.0.0" {
    return $sdkRoot -and (Test-Path -LiteralPath (Join-Path $sdkRoot "build-tools\36.0.0"))
} "Install Android SDK Build-Tools 36.0.0."

Test-Check "Practice AVD $avdName" {
    if (-not (Test-Path -LiteralPath $config)) {
        return $false
    }
    $text = Get-Content -Raw -LiteralPath $config
    if ($text -notmatch "(?m)^abi.type\s*=\s*$([regex]::Escape($abi))\s*$") {
        return $false
    }
    if ($text -notmatch "(?m)^image.sysdir.1\s*=.*android-36.*google_apis.*$([regex]::Escape($abi))") {
        return $false
    }
    $ram = [regex]::Match($text, '(?m)^hw.ramSize\s*=\s*([^\r\n]+)').Groups[1].Value.Trim()
    $ramNumber = [int]($ram -replace '[^0-9]', '')
    if ($ram -match 'G$') {
        $ramNumber *= 1024
    }
    return $ramNumber -ge 1536 -and $ramNumber -le 4096
} "Run .\scripts\setup-emulator.ps1 to create the API 36 AVD with 2 GB RAM."

Test-Check "Booted practice emulator" {
    if (-not $sdkRoot) {
        return $false
    }
    $adb = Join-Path $sdkRoot "platform-tools\adb.exe"
    if (-not (Test-Path -LiteralPath $adb)) {
        return $false
    }
    $serials = & $adb devices |
        Select-String '^emulator-\d+\s+device$' |
        ForEach-Object { ($_.Line -split '\s+')[0] }
    foreach ($serial in $serials) {
        $runningName = ((& $adb -s $serial emu avd name 2>$null | Select-Object -First 1) -join "").Trim()
        $booted = ((& $adb -s $serial shell getprop sys.boot_completed 2>$null) -join "").Trim()
        if ($runningName -eq $avdName -and $booted -eq "1") {
            return $true
        }
    }
    return $false
} "Run .\scripts\setup-emulator.ps1 and wait for its boot confirmation."

Test-Check "Pinned npm toolchain installed" {
    return (Test-Path -LiteralPath "node_modules\.bin\appium.cmd") -and (Test-Path -LiteralPath "node_modules\.bin\allure.cmd")
} "Run npm.cmd ci from the repository root."

Test-Check "Appium 2.16.2" {
    if (-not (Test-Path -LiteralPath "node_modules\.bin\appium.cmd")) {
        return $false
    }
    return ((& .\node_modules\.bin\appium.cmd --version) -join "").Trim() -eq "2.16.2"
} "Run npm.cmd ci; do not install a global Appium version for this repository."

Test-Check "UiAutomator2 driver 3.9.8" {
    if (-not (Test-Path -LiteralPath "node_modules\.bin\appium.cmd")) {
        return $false
    }
    $drivers = ((& .\node_modules\.bin\appium.cmd driver list --installed --json 2>&1) -join "`n") | ConvertFrom-Json
    return ($drivers.PSObject.Properties.Name -contains "uiautomator2") -and ($drivers.uiautomator2.version -eq "3.9.8")
} "Run npm.cmd ci from the repository root, then check npx.cmd --no-install appium driver list --installed."

Test-Check "Allure CLI 2.43.0" {
    if (-not (Test-Path -LiteralPath "node_modules\.bin\allure.cmd")) {
        return $false
    }
    return ((& .\node_modules\.bin\allure.cmd --version) -join "").Trim() -eq "2.43.0"
} "Run npm.cmd ci to restore the pinned Allure CLI."

Write-Host "Environment check summary"
Write-Host "Passed: $passed, failed: $failed"
if ($failed -gt 0) {
    exit 1
}
Write-Host "Environment is ready. Next: start fake-api and .\scripts\start-appium.ps1, then run .\scripts\run-suite.ps1"
