package com.sandbox.qa.fakeapi

import kotlin.random.Random

/**
 * Latency and failure profile of the fake backend. The defaults mirror
 * The product's deterministic network profile: a normal call takes 600 ms,
 * slow_backend_response pins every call at 8000 ms, intermittent_backend_delay
 * spikes about half of the calls to 18000 ms (a genuine flake by design).
 *
 * Module tests inject zero delays and a seeded [random] to stay fast and
 * deterministic; runtime defaults exercise realistic timeout behavior.
 */
class NetworkSimulation(
    val baseDelayMs: Long = 600,
    val slowDelayMs: Long = 8_000,
    val spikeDelayMs: Long = 18_000,
    val random: Random = Random.Default,
)

/** Thrown by the simulated backend when backend_error is enabled; mapped to HTTP 500. */
class BackendErrorException : RuntimeException("HTTP 500: Internal Server Error")
