# fake-api

Local deterministic ride-hailing REST API used by both the Android app and the API testing track. It has the same product data and six sandbox states as the mobile UI, but all product data calls cross a real HTTP boundary.

Fully self contained: no database, no external services. Seed data and sandbox states live in memory and reset on server restart.

## Run

```
./gradlew :fake-api:run          # gradlew.bat on Windows
```

The server listens on port 8080; override with the `FAKE_API_PORT` environment variable. Module self tests: `./gradlew :fake-api:test`.

Interactive Swagger UI: [http://localhost:8080/swagger](http://localhost:8080/swagger). The root URL redirects there.

The Android Emulator reaches the host API at `http://10.0.2.2:8080`; the app has this URL in `BuildConfig.API_BASE_URL`.

## Quick tour

```
# 1. Request an OTP (any phone with at least 8 digits; the valid code is always 1234)
curl -s -X POST localhost:8080/auth/phone -H "Content-Type: application/json" -d '{"phone":"+381 64 123 45 67"}'

# 2. Exchange the OTP for a bearer token
curl -s -X POST localhost:8080/auth/otp -H "Content-Type: application/json" -d '{"phone":"+381 64 123 45 67","code":"1234"}'

# 3. Call the data endpoints with the token
curl -s "localhost:8080/rides/options?from=Center&to=Airport" -H "Authorization: Bearer <token>"
curl -s localhost:8080/orders -H "Authorization: Bearer <token>"

# 4. Order a ride, read the active ride, then complete it; it becomes the newest history entry
curl -s -X POST localhost:8080/rides -H "Content-Type: application/json" -H "Authorization: Bearer <token>" -d '{"from":"Oak Avenue","to":"Market Street","rideOptionId":1}'
curl -s localhost:8080/rides/active -H "Authorization: Bearer <token>"
curl -s -X POST localhost:8080/rides/100/complete -H "Authorization: Bearer <token>"
```

## Endpoints

| Method | Path | Auth | Purpose |
| --- | --- | --- | --- |
| POST | /auth/phone | no | Validate the phone (>= 8 digits), issue an OTP |
| POST | /auth/otp | no | Validate the phone (>= 8 digits), exchange code 1234 for a session-scoped bearer token |
| GET | /rides/options?from=&to= | bearer | Three tariffs in euro cents: Yellow 2970, Turquoise 3454, Minivan 3905 |
| GET | /rides/active | bearer | Return the one active ride for this sandbox session, or HTTP 404 |
| POST | /rides | bearer | Search for a driver; HTTP 409 with `ACTIVE_RIDE_EXISTS` if the session already has an active ride |
| POST | /rides/{id}/complete | bearer | Complete a ride and add it to order history |
| POST | /rides/{id}/cancel | bearer | Cancel a ride without a history entry |
| GET | /orders | bearer | Past orders in euro cents: 2970 / 1450 / 980 |
| POST | /location/resolve | bearer | Resolve the deterministic current location |
| GET | /region/status | no | `{"available": true}` unless region_unavailable is enabled |
| GET | /sandbox/state | no | Snapshot of all six sandbox states |
| POST | /sandbox/state | no | Enable or disable one state |
| POST | /sandbox/state/snapshot | no | Replace all six states from the Android adb-controlled snapshot |
| POST | /sandbox/reset | no | Disable states, clear active rides, restore seed history; issued tokens remain valid |

The full contract with schemas and examples is [openapi.yaml](openapi.yaml).

An active ride has no TTL and survives client restarts. Only complete, cancel
and sandbox reset release the session's active slot.

Bearer tokens are scoped to `X-Sandbox-Session`. Send the same session header
to `/auth/otp` and every protected request; omitting it consistently uses the
shared default session. Sandbox reset restores product data but is not logout.

## Sandbox states over HTTP

The same six product failure states as in the app (`ConditionConfig.ALL`), toggled over HTTP instead of an adb broadcast:

```
curl -s -X POST localhost:8080/sandbox/state -H "Content-Type: application/json" -d '{"condition":"backend_error","enabled":true}'
curl -s -X POST localhost:8080/sandbox/reset
```

| State | Observable effect |
| --- | --- |
| slow_backend_response | Every data call takes 8000 ms (normal is 600 ms) |
| backend_error | Every data call returns HTTP 500 |
| car_unavailable | The Minivan tariff (id 3) stays listed with `available=false` |
| driver_not_found | POST /rides returns HTTP 409 and does not create an active ride |
| intermittent_backend_delay | About half of the data calls spike to 18000 ms, a genuine flake |
| region_unavailable | GET /region/status flips to `{"available": false}` |

Auth and the /sandbox control plane always answer fast; data endpoints,
including `/rides/active`, go through the simulated network. A 200 from POST
`/sandbox/state` only means the flag flipped: verify the observable effect, the
same verification rule used for the adb broadcast.

## Load-bearing values

The seed data mirrors the app (see "Load-bearing values" in the root AGENTS.md): OTP 1234, phone needs at least 8 digits, tariff euro cents 2970 / 3454 / 3905, order euro cents 2970 / 1450 / 980, Minivan id 3. API and UI tests assert these wire values. The mobile UI formats them as `29.70 €` / `34.54 €` / `39.05 €` and `29.70 €` / `14.50 €` / `9.80 €`.
