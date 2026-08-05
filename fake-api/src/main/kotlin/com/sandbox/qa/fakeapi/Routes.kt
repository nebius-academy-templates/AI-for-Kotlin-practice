package com.sandbox.qa.fakeapi

import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.install
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.plugins.swagger.swaggerUI
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondRedirect
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import java.util.UUID

/**
 * The whole HTTP surface of the fake ride-hailing API.
 *
 * Auth is deliberately simple but real enough to test: a valid phone (at
 * least [SeedData.MIN_PHONE_DIGITS] digits) plus the valid OTP issue a bearer
 * token, and the data endpoints reject calls without one. Tokens are scoped
 * to a backend session and live in memory for the server process lifetime.
 *
 * Sandbox states change what the data endpoints do (latency, HTTP 500, a
 * disabled tariff, no driver, an unavailable region). The control plane under /sandbox is
 * a testability seam, not product surface, so it needs no auth, mirroring the
 * adb broadcast in the mobile app.
 */
fun Application.fakeApiModule(
    states: SandboxStates = SandboxStates(),
    network: NetworkSimulation = NetworkSimulation(),
    rideStore: RideStore = RideStore(),
) {
    val sessions = BackendSessions(states, rideStore)

    install(ContentNegotiation) {
        json(
            Json {
                ignoreUnknownKeys = true
                // Encode defaults so RideOption.available=true is always on the wire,
                // not only when car_unavailable flips it to false.
                encodeDefaults = true
            },
        )
    }
    install(CallLogging)
    install(StatusPages) {
        exception<BackendErrorException> { call, _ ->
            call.respond(HttpStatusCode.InternalServerError, ErrorResponse("Internal Server Error"))
        }
        exception<BadRequestException> { call, _ ->
            call.respond(HttpStatusCode.BadRequest, ErrorResponse("Malformed request body"))
        }
    }

    // Applied to data endpoints only; auth and the /sandbox control plane
    // always answer fast.
    suspend fun simulateNetwork(states: SandboxStates) {
        var latency = if (states.isEnabled(SandboxStates.SLOW_BACKEND_RESPONSE)) network.slowDelayMs else network.baseDelayMs
        // intermittent_backend_delay: genuinely intermittent, about half of
        // the calls spike past typical client timeouts.
        if (states.isEnabled(SandboxStates.INTERMITTENT_BACKEND_DELAY) && network.random.nextBoolean()) {
            latency = network.spikeDelayMs
        }
        delay(latency)
        if (states.isEnabled(SandboxStates.BACKEND_ERROR)) {
            throw BackendErrorException()
        }
    }

    routing {
        get("/") {
            call.respondRedirect("/swagger")
        }
        swaggerUI(path = "swagger", swaggerFile = "openapi.yaml")

        post("/auth/phone") {
            val body = call.receive<PhoneRequest>()
            if (body.phone.count { it.isDigit() } < SeedData.MIN_PHONE_DIGITS) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    ErrorResponse(SeedData.PHONE_VALIDATION_ERROR),
                )
                return@post
            }
            call.respond(StatusResponse("otp_sent"))
        }

        post("/auth/otp") {
            val body = call.receive<OtpRequest>()
            if (body.phone.count { it.isDigit() } < SeedData.MIN_PHONE_DIGITS) {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse(SeedData.PHONE_VALIDATION_ERROR))
                return@post
            }
            if (body.code != SeedData.VALID_OTP) {
                // Same copy as the mobile UI error (WRONG_OTP_ERROR in the
                // appium-tests test data): one product, one wording.
                call.respond(HttpStatusCode.Unauthorized, ErrorResponse("Invalid code"))
                return@post
            }
            val token = "sandbox-${UUID.randomUUID()}"
            call.backendSession(sessions).issuedTokens.add(token)
            call.respond(TokenResponse(token))
        }

        get("/rides/options") {
            if (!call.requireToken(sessions)) {
                return@get
            }
            val from = call.request.queryParameters["from"]
            val to = call.request.queryParameters["to"]
            if (from.isNullOrBlank() || to.isNullOrBlank()) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    ErrorResponse("Query parameters 'from' and 'to' are required"),
                )
                return@get
            }
            val session = call.backendSession(sessions)
            simulateNetwork(session.states)
            val options =
                if (session.states.isEnabled(SandboxStates.CAR_UNAVAILABLE)) {
                    // The tariff stays in the catalog, grayed: available=false.
                    SeedData.rideOptions.map {
                        if (it.id == SeedData.MINIVAN_RIDE_ID) it.copy(available = false) else it
                    }
                } else {
                    SeedData.rideOptions
                }
            call.respond(RideOptionsResponse(options))
        }

        get("/rides/active") {
            if (!call.requireToken(sessions)) {
                return@get
            }
            val session = call.backendSession(sessions)
            simulateNetwork(session.states)
            val activeRide = session.rides.active()
            if (activeRide == null) {
                call.respond(HttpStatusCode.NotFound, ErrorResponse("No active ride"))
                return@get
            }
            call.respond(activeRide)
        }

        post("/rides") {
            if (!call.requireToken(sessions)) {
                return@post
            }
            val body = call.receive<CreateRideRequest>()
            if (body.from.isBlank() || body.to.isBlank()) {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse("Both from and to are required"))
                return@post
            }
            val session = call.backendSession(sessions)
            simulateNetwork(session.states)
            if (session.states.isEnabled(SandboxStates.REGION_UNAVAILABLE)) {
                call.respond(HttpStatusCode.Conflict, ErrorResponse("Service is not available in this region yet"))
                return@post
            }
            val option = SeedData.rideOptions.firstOrNull { it.id == body.rideOptionId }
            if (option == null) {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse("Unknown ride option ${body.rideOptionId}"))
                return@post
            }
            if (session.rides.active() != null) {
                call.respondActiveRideConflict()
                return@post
            }
            if (session.states.isEnabled(SandboxStates.CAR_UNAVAILABLE) && option.id == SeedData.MINIVAN_RIDE_ID) {
                call.respond(HttpStatusCode.Conflict, ErrorResponse("Selected ride option is unavailable"))
                return@post
            }
            if (session.states.isEnabled(SandboxStates.DRIVER_NOT_FOUND)) {
                call.respond(HttpStatusCode.Conflict, ErrorResponse("No cars found for this route"))
                return@post
            }
            val ride = session.rides.create(body.from, body.to, option)
            if (ride == null) {
                // create() repeats the check while holding the RideStore lock,
                // so concurrent requests cannot both occupy the active slot.
                call.respondActiveRideConflict()
                return@post
            }
            call.respond(HttpStatusCode.Created, ride)
        }

        post("/rides/{id}/complete") {
            if (!call.requireToken(sessions)) {
                return@post
            }
            val session = call.backendSession(sessions)
            simulateNetwork(session.states)
            val id = call.parameters["id"]?.toIntOrNull()
            val completed = id?.let(session.rides::complete)
            if (completed == null) {
                call.respond(HttpStatusCode.Conflict, ErrorResponse("Ride is missing or cannot be completed"))
                return@post
            }
            call.respond(completed)
        }

        post("/rides/{id}/cancel") {
            if (!call.requireToken(sessions)) {
                return@post
            }
            val session = call.backendSession(sessions)
            simulateNetwork(session.states)
            val id = call.parameters["id"]?.toIntOrNull()
            val cancelled = id?.let(session.rides::cancel)
            if (cancelled == null) {
                call.respond(HttpStatusCode.Conflict, ErrorResponse("Ride is missing or cannot be cancelled"))
                return@post
            }
            call.respond(cancelled)
        }

        get("/orders") {
            if (!call.requireToken(sessions)) {
                return@get
            }
            val session = call.backendSession(sessions)
            simulateNetwork(session.states)
            call.respond(OrdersResponse(session.rides.orders()))
        }

        post("/location/resolve") {
            if (!call.requireToken(sessions)) {
                return@post
            }
            val session = call.backendSession(sessions)
            simulateNetwork(session.states)
            call.respond(LocationResponse("Oak Avenue"))
        }

        // Reads the state registry directly and never goes through
        // simulateNetwork, mirroring the region banner in the app, which
        // renders straight from ConditionConfig without a backend call.
        get("/region/status") {
            val session = call.backendSession(sessions)
            call.respond(RegionStatus(available = !session.states.isEnabled(SandboxStates.REGION_UNAVAILABLE)))
        }

        route("/sandbox") {
            get("/state") {
                call.respond(call.backendSession(sessions).states.snapshot())
            }
            post("/state") {
                val session = call.backendSession(sessions)
                val body = call.receive<SandboxStateRequest>()
                if (!session.states.set(body.condition, body.enabled)) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        ErrorResponse("Unknown condition '${body.condition}'. Known conditions: ${SandboxStates.ALL.joinToString(", ")}"),
                    )
                    return@post
                }
                call.respond(body)
            }
            post("/state/snapshot") {
                val session = call.backendSession(sessions)
                val body = call.receive<Map<String, Boolean>>()
                if (!session.states.replace(body)) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        ErrorResponse("Snapshot must contain exactly: ${SandboxStates.ALL.joinToString(", ")}"),
                    )
                    return@post
                }
                call.respond(session.states.snapshot())
            }
            post("/reset") {
                val session = call.backendSession(sessions)
                session.states.reset()
                session.rides.reset()
                call.respond(StatusResponse("reset"))
            }
        }
    }
}

private fun ApplicationCall.backendSession(sessions: BackendSessions): BackendSession =
    sessions.get(request.headers[BackendSessions.HEADER])

private suspend fun ApplicationCall.requireToken(sessions: BackendSessions): Boolean {
    val header = request.headers[HttpHeaders.Authorization]
    val token = header?.takeIf { it.startsWith("Bearer ") }?.removePrefix("Bearer ")
    if (token == null || token !in backendSession(sessions).issuedTokens) {
        respond(HttpStatusCode.Unauthorized, ErrorResponse("Missing or invalid token"))
        return false
    }
    return true
}

private suspend fun ApplicationCall.respondActiveRideConflict() {
    respond(
        HttpStatusCode.Conflict,
        CodedErrorResponse(
            error = "An active ride already exists",
            code = RideStore.ACTIVE_RIDE_EXISTS_CODE,
        ),
    )
}
