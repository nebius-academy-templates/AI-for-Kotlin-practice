[CmdletBinding()]
param(
    [ValidateSet("stable", "redesign")]
    [string]$Flavor = "stable",
    [string]$Device = "",
    [string]$TestFilter = ""
)

$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

$workspace = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
Set-Location -LiteralPath $workspace
$sdkRoot = if ($env:ANDROID_HOME) { $env:ANDROID_HOME } elseif ($env:ANDROID_SDK_ROOT) { $env:ANDROID_SDK_ROOT } else { Join-Path $env:LOCALAPPDATA "Android\Sdk" }
$adb = Join-Path $sdkRoot "platform-tools\adb.exe"
$gradle = Join-Path $workspace "gradlew.bat"
$localAppium = Join-Path $workspace "node_modules\.bin\appium.cmd"
$localAllure = Join-Path $workspace "node_modules\.bin\allure.cmd"

function Test-HttpReady([string]$Url) {
    try {
        return (Invoke-WebRequest -UseBasicParsing $Url -TimeoutSec 2).StatusCode -eq 200
    } catch {
        return $false
    }
}

if (-not (Test-Path -LiteralPath $adb)) {
    throw "adb was not found at $adb. Run .\scripts\bootstrap.ps1."
}
if (-not (Test-Path -LiteralPath $localAppium) -or -not (Test-Path -LiteralPath $localAllure)) {
    throw "The pinned local Appium/Allure toolchain is missing. Run npm.cmd ci in $workspace."
}
if (-not (Test-HttpReady "http://127.0.0.1:8080/swagger")) {
    throw "fake-api is not ready. Start it in another PowerShell window: .\gradlew.bat :fake-api:run"
}
if (-not (Test-HttpReady "http://127.0.0.1:4723/status")) {
    throw "Appium is not ready. Start it in another PowerShell window: .\scripts\start-appium.ps1"
}
$appiumStatus = (Invoke-WebRequest -UseBasicParsing "http://127.0.0.1:4723/status" -TimeoutSec 2).Content | ConvertFrom-Json
$serverVersion = $appiumStatus.value.build.version
if ($serverVersion -ne "2.16.2") {
    throw "Appium server version is $serverVersion; this repository requires pinned 2.16.2. Stop it and run .\scripts\start-appium.ps1 from $workspace."
}

$connected = @(& $adb devices |
    Select-String '^\S+\s+device$' |
    ForEach-Object { ($_.Line -split '\s+')[0] })
if ($Device) {
    if ($Device -notin $connected) {
        throw "Requested device $Device is not connected. Connected devices: $($connected -join ', ')"
    }
} elseif ($connected.Count -eq 1) {
    $Device = $connected[0]
} elseif ($connected.Count -eq 0) {
    throw "No booted emulator is connected. Run .\scripts\setup-emulator.ps1."
} else {
    throw "More than one device is connected: $($connected -join ', '). Pass -Device SERIAL explicitly."
}

if ((& $adb -s $Device shell getprop sys.boot_completed).Trim() -ne "1") {
    throw "Device $Device has not completed boot."
}
foreach ($setting in "window_animation_scale", "transition_animation_scale", "animator_duration_scale") {
    & $adb -s $Device shell settings put global $setting 0 | Out-Null
}

$flavorTitle = (Get-Culture).TextInfo.ToTitleCase($Flavor)
Write-Host "Building $Flavor debug APK"
& $gradle ":app:assemble${flavorTitle}Debug"
if ($LASTEXITCODE -ne 0) {
    exit $LASTEXITCODE
}

Write-Host "Resetting sandbox states on $Device"
& $adb -s $Device shell am broadcast -n com.sandbox.qa/.condition.ConditionReceiver --ez reset true | Out-Null

$resultsDir = [IO.Path]::GetFullPath((Join-Path $workspace "appium-tests\build\allure-results"))
$reportDir = [IO.Path]::GetFullPath((Join-Path $workspace "appium-tests\build\reports\allure-report"))
$junitDir = [IO.Path]::GetFullPath((Join-Path $workspace "appium-tests\build\test-results\test"))
foreach ($target in @($resultsDir, $reportDir, $junitDir)) {
    if (-not $target.StartsWith($workspace + [IO.Path]::DirectorySeparatorChar)) {
        throw "Unsafe report cleanup target: $target"
    }
    if (Test-Path -LiteralPath $target) {
        Remove-Item -LiteralPath $target -Recurse -Force
    }
}

$apk = Join-Path $workspace "app\build\outputs\apk\$Flavor\debug\app-$Flavor-debug.apk"
$gradleArgs = @(
    ":appium-tests:test",
    "--rerun",
    "-Dapp.apk=$apk",
    "-Dui.variant=$Flavor",
    "-Dappium.devices=$Device"
)
if ($TestFilter) {
    $gradleArgs += @("--tests", $TestFilter)
}

$suiteLog = Join-Path $workspace "suite-run.log"
Write-Host "Running Appium suite on $Device"
& $gradle @gradleArgs 2>&1 | Tee-Object -FilePath $suiteLog
$gradleExit = $LASTEXITCODE

$resultFiles = @(Get-ChildItem (Join-Path $workspace "appium-tests\build\test-results\test") -Filter "TEST-*.xml" -File -ErrorAction SilentlyContinue)
$tests = 0
$failures = 0
$errors = 0
$skipped = 0
foreach ($file in $resultFiles) {
    [xml]$xml = Get-Content -Raw -LiteralPath $file.FullName
    $tests += [int]$xml.testsuite.tests
    $failures += [int]$xml.testsuite.failures
    $errors += [int]$xml.testsuite.errors
    $skipped += [int]$xml.testsuite.skipped
}
$passed = $tests - $failures - $errors - $skipped
$summary = "Appium result: $passed passed, $failures failed, $errors errors, $skipped skipped ($tests total) on $Device"
$summary | Tee-Object -FilePath $suiteLog -Append

if (Test-Path -LiteralPath $resultsDir) {
    & $localAllure generate $resultsDir -o $reportDir
    if ($LASTEXITCODE -eq 0 -and (Test-Path -LiteralPath (Join-Path $reportDir "index.html"))) {
        Write-Host "Allure report: $(Join-Path $reportDir 'index.html')"
    } else {
        Write-Warning "Allure report generation failed; raw results remain in $resultsDir"
    }
}

if ($tests -eq 0) {
    Write-Error "No tests executed. The run is not valid proof."
    exit 1
}
exit $gradleExit
