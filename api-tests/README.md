# API tests

Kotlin, JUnit 5 and REST Assured tests for the HTTP contract in
`fake-api/openapi.yaml`. The starter contains four tests in `ApiSmokeTest`
and `RideLifecycleApiTest`.

## Run

Start the API and run the tests in separate terminals:

```bash
./gradlew :fake-api:run
./gradlew :api-tests:test --rerun
```

On Windows use `gradlew.bat`. Override the target with
`-Dapi.url=http://host:port` when necessary.

## Architecture

- `rule/`: shared setup, isolated sandbox sessions, reset and token helpers.
- `client/`: REST Assured requests and response decoding, no assertions.
- `model/`: request and response DTOs owned by the test module.
- `tests/`: scenarios, Allure steps and assertions.
- `testdata/`: shared wire-level values.

Tests do not import backend domain classes: the API remains a black box. Every
request and response is attached to Allure through the shared REST Assured
filter. New tests use camelCase methods, `@DisplayName`, `@AllureId`, `@Feature`
and named `step` blocks.

Raw Allure results are written to `api-tests/build/allure-results`.
