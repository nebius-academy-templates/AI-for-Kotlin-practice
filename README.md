# AI for Kotlin Practice

Practice repository for AI-assisted QA work. The system under test is a Kotlin
ride-hailing Android app in `app/`, backed by the deterministic local Ktor API
in `fake-api/`.

## Repository layout

| Path | Purpose |
| --- | --- |
| `app/` | Android application under test, with stable and redesign flavors |
| `appium-tests/` | Kotlin, JUnit 5 and Appium UI automation |
| `api-tests/` | Kotlin, JUnit 5 and REST Assured API automation |
| `agent_docs/` | Repository context for build commands, test architecture and page-object conventions |
| `fake-api/` | Local backend, OpenAPI contract and Swagger UI |
| `scripts/` | Environment setup and sequential Appium runners |

The starter contains six Appium test cases and four API test cases. Add new
coverage in the test modules; do not edit `app/` to make a failing test green.

## Agent skills

Shared skills are authored in `.agents/skills`. The matching files under
`.claude/skills` are generated mirrors for Claude Code. After changing a
canonical skill, synchronize the mirror with:

```bash
python scripts/sync_agent_skills.py
```

CI runs the same command with `--check` and fails when the copies differ.

## Prerequisites

- JDK 17
- Node.js 20 or newer
- Android SDK platform 36, build-tools 36.0.0 and platform-tools
- an API 36 Android emulator

Gradle is supplied by the wrapper. Appium 2, UiAutomator2 and Allure are pinned
in `package-lock.json`; install them with `npm ci` and do not upgrade the
toolchain independently.

## Quick start

Windows PowerShell:

```powershell
npm.cmd ci
.\scripts\setup-emulator.ps1
.\scripts\bootstrap.ps1
.\gradlew.bat :fake-api:run
```

Keep the backend running. In another terminal, start Appium and run the UI
suite:

```powershell
.\scripts\start-appium.ps1
.\scripts\run-suite.ps1
```

Run the API suite from another terminal:

```powershell
.\gradlew.bat :api-tests:test --rerun
```

macOS or Linux:

```bash
npm ci
./scripts/setup-emulator.sh
./scripts/bootstrap.sh
./gradlew :fake-api:run
./scripts/start-appium.sh
./scripts/run-suite.sh
./gradlew :api-tests:test --rerun
```

Swagger UI is available at `http://localhost:8080/swagger` while `fake-api` is
running.

## Sandbox states

The app exposes six deterministic or controlled states:
`slow_backend_response`, `backend_error`, `car_unavailable`,
`driver_not_found`, `intermittent_backend_delay` and `region_unavailable`.
They can be enabled in Settings or through the component-targeted adb control
seam documented in `AGENTS.md`.

## CI scope

GitHub Actions checks Kotlin formatting, builds both app flavors, runs the API
starter suite and compiles the Appium module. It does not run Appium tests.
A local emulator run through the OS-specific suite runner is required as UI
test evidence.

See `appium-tests/README.md`, `api-tests/README.md` and `AGENTS.md` for the test
architecture and repository rules.
