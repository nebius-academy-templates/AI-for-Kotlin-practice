package com.sandbox.qa.condition

import androidx.compose.runtime.mutableStateMapOf

/**
 * In-memory sandbox state registry.
 *
 * Each id is a controlled product failure state of the fake backend
 * (unavailable region, missing tariff, degraded backend). States are
 * toggled from the Settings screen or via adb broadcast (see
 * [ConditionReceiver]). State is observable by Compose, so enabling a
 * state takes effect immediately without restarting the app.
 */
object ConditionConfig {
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

    private val state =
        mutableStateMapOf<String, Boolean>().apply {
            ALL.forEach { put(it, false) }
        }

    /**
     * Creates and validates the observable registry on the main thread before
     * the first composition. Do not remove this eager access: initializing the
     * snapshot state for the first time from [ConditionReceiver] can crash.
     */
    fun initialize() {
        check(state.size == ALL.size && ALL.all(state::containsKey)) {
            "Sandbox state registry does not match the known conditions"
        }
    }

    fun isEnabled(condition: String): Boolean = state[condition] == true

    fun set(
        condition: String,
        enabled: Boolean,
    ) {
        if (condition in ALL) {
            state[condition] = enabled
        }
    }

    fun reset() {
        ALL.forEach { state[it] = false }
    }

    fun snapshot(): Map<String, Boolean> = ALL.associateWith(::isEnabled)
}
