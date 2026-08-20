---
name: run-appium-suite
description: >-
  Build the Android APK and run the Appium UI suite on one emulator using the
  repository's PowerShell or bash runner. Use when asked to run UI tests,
  verify the suite, or collect a local Appium result. Checks the real run
  result in the root suite-run.log on every supported OS.
---

# Run the Appium suite

Use `scripts/run-suite.ps1` on Windows or `scripts/run-suite.sh` on macOS and
Linux. The runner builds the selected APK, selects one emulator, disables
animations, resets sandbox state, forces a real test execution and generates
JUnit and Allure reports.

## Prerequisites

Install the pinned toolchain and verify the environment:

```powershell
npm.cmd ci
.\scripts\setup-emulator.ps1
.\scripts\bootstrap.ps1
```

```bash
npm ci
./scripts/setup-emulator.sh
./scripts/bootstrap.sh
```

Keep `fake-api` and Appium running in separate terminals.

Windows:

```powershell
.\gradlew.bat :fake-api:run
.\scripts\start-appium.ps1
```

macOS or Linux:

```bash
./gradlew :fake-api:run
./scripts/start-appium.sh
```

## Run

Stable flavor:

```powershell
.\scripts\run-suite.ps1
```

```bash
./scripts/run-suite.sh
```

Redesign flavor:

```powershell
.\scripts\run-suite.ps1 -Flavor redesign
```

```bash
FLAVOR=redesign ./scripts/run-suite.sh
```

When several devices are connected, pass `-Device <serial>` on Windows. Run a
single class or method with `-TestFilter <filter>` on Windows or a Gradle
`--tests <filter>` argument through the bash runner.

## Verify

Read the final `Appium result` line in `suite-run.log` and confirm that the
executed test count is non-zero. Derive the expected inventory from the `@Test`
methods under `appium-tests/src/test/kotlin/tests/` and compare it with the
JUnit XML totals. The runner always passes `--rerun`; a cached `UP-TO-DATE`
result is not a test execution.

Detailed results are available in:

- `appium-tests/build/test-results/test` for JUnit XML;
- `appium-tests/build/reports/tests/test/index.html` for the Gradle report;
- `appium-tests/build/reports/allure-report/index.html` for Allure;
- `appium-tests/build/reports/failures` for failure artifacts.
