package com.sandbox.qa.di

import android.content.Context

/** Persists the last location resolved during onboarding for the ride form. */
class LocationStore(
    context: Context,
) {
    private val preferences =
        context.getSharedPreferences("sandbox_location", Context.MODE_PRIVATE)

    fun currentPickup(): String = preferences.getString(KEY_PICKUP, DEFAULT_PICKUP).orEmpty()

    fun savePickup(value: String) {
        preferences.edit().putString(KEY_PICKUP, value).apply()
    }

    fun clear() {
        preferences.edit().clear().apply()
    }

    companion object {
        const val DEFAULT_PICKUP = "Oak Avenue"
        private const val KEY_PICKUP = "pickup"
    }
}
