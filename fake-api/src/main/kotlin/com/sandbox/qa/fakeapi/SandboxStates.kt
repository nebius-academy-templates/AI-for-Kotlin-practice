package com.sandbox.qa.fakeapi

import java.util.concurrent.ConcurrentHashMap

/**
 * In-memory sandbox state registry, the HTTP counterpart of the app's
 * ConditionConfig. Same six state ids, same semantics; toggled over HTTP
 * (POST /sandbox/state, POST /sandbox/reset) instead of an adb broadcast.
 *
 * The registry is per server process: restarting the server resets every
 * state, just like the per-session app process restart does in the mobile
 * suite.
 */
class SandboxStates {
    companion object {
        const val SLOW_BACKEND_RESPONSE = "slow_backend_response"
        const val BACKEND_ERROR = "backend_error"
        const val CAR_UNAVAILABLE = "car_unavailable"
        const val DRIVER_NOT_FOUND = "driver_not_found"
        const val INTERMITTENT_BACKEND_DELAY = "intermittent_backend_delay"
        const val REGION_UNAVAILABLE = "region_unavailable"

        val ALL =
            listOf(
                SLOW_BACKEND_RESPONSE,
                BACKEND_ERROR,
                CAR_UNAVAILABLE,
                DRIVER_NOT_FOUND,
                INTERMITTENT_BACKEND_DELAY,
                REGION_UNAVAILABLE,
            )
    }

    private val state = ConcurrentHashMap(ALL.associateWith { false })

    fun isEnabled(condition: String): Boolean = state[condition] == true

    /** Returns false when [condition] is not a known state id. */
    fun set(
        condition: String,
        enabled: Boolean,
    ): Boolean {
        if (condition !in ALL) {
            return false
        }
        state[condition] = enabled
        return true
    }

    fun reset() {
        ALL.forEach { state[it] = false }
    }

    /** Atomically replace the full state snapshot supplied by the mobile app. */
    @Synchronized
    fun replace(snapshot: Map<String, Boolean>): Boolean {
        if (snapshot.keys != ALL.toSet()) {
            return false
        }
        ALL.forEach { state[it] = snapshot[it] == true }
        return true
    }

    fun snapshot(): Map<String, Boolean> = ALL.associateWith { state[it] == true }
}
