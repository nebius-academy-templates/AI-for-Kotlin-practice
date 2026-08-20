package com.sandbox.qa.fakeapi

import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty

/**
 * Entry point of the deterministic ride-hailing REST API used by the mobile
 * app and the API testing track.
 *
 * Start with `./gradlew :fake-api:run`. The port defaults to 8080 and can be
 * overridden with the FAKE_API_PORT environment variable. Fully self
 * contained: no database, no external services, seed data and sandbox states
 * live in memory and reset on restart.
 */
fun main() {
    val port = System.getenv("FAKE_API_PORT")?.toIntOrNull() ?: 8080
    embeddedServer(Netty, port = port, host = "0.0.0.0") { fakeApiModule() }
        .start(wait = true)
}
