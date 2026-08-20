# Appium tests

Kotlin, JUnit 5 and Appium UiAutomator2 tests for the Android app in `app/`.
The starter suite contains six test cases in three classes:
`OnboardingSmokeTest`, `PasskeyPromoTest` and `RideAndHistoryE2ETest`.

## Run

Provision the emulator once, install the pinned Node toolchain, start the
backend and Appium, then use the repository runner.

Windows PowerShell:

```powershell
.\scripts\setup-emulator.ps1
npm.cmd ci
.\gradlew.bat :fake-api:run
.\scripts\start-appium.ps1
.\scripts\run-suite.ps1
.\scripts\run-suite.ps1 -Flavor redesign
```

macOS or Linux:

```bash
./scripts/setup-emulator.sh
npm ci
./gradlew :fake-api:run
./scripts/start-appium.sh
./scripts/run-suite.sh
FLAVOR=redesign ./scripts/run-suite.sh
```

Do not use a bare cached `:appium-tests:test` result as evidence. The runners
pass `--rerun`, configure the APK and device, reset sandbox state, disable
animations and generate JUnit and Allure reports.

## Architecture

```text
src/test/kotlin/
  rule/       session lifecycle, driver configuration and failure evidence
  pages/      singleton Element catalogs, no assertions
  actions/    interactions, flows, waits and assertions
  tests/      JUnit scenarios composed from actions
  testdata/   shared test values
```

Use `AppiumBy.id` with stable `testTag` resource IDs. Pages do not assert or
own flows. Tests use camelCase methods, `@DisplayName`, `@AllureId`, `@Feature`
and named `step` blocks. `Thread.sleep` and text XPath locators are forbidden.

## Evidence

- JUnit XML: `appium-tests/build/test-results/test`
- Gradle HTML: `appium-tests/build/reports/tests/test/index.html`
- Allure results: `appium-tests/build/allure-results`
- Static Allure report: `appium-tests/build/reports/allure-report/index.html`
- Failure artifacts: `appium-tests/build/reports/failures`
- Failure digests: `appium-tests/build/reports/digests`

`FailureDigest` puts the exception, project stack, raw-artifact paths, visible
resource IDs from the captured page source, and a focused logcat slice in the
digest. Read it first; open the raw artifacts when it is not decisive.

CI compiles this module but does not execute Appium tests. Run the suite on the
local API 36 emulator for UI evidence.
