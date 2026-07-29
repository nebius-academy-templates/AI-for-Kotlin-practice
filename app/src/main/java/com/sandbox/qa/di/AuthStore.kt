package com.sandbox.qa.di

import android.content.Context

/**
 * Persistent "the user has signed in" flag. Unlike the rest of the sandbox
 * state it survives process restarts on purpose: a returning user must land
 * on the ride form, not on the login screen.
 *
 * The test seam: the ConditionReceiver reset broadcast
 * (`--ez reset true`) clears this flag together with the sandbox states, and
 * the test base class sends that broadcast after every test - so autotests
 * always start from a known entrance while manual sessions keep their login.
 */
class AuthStore(
    context: Context,
) {
    private val prefs = context.getSharedPreferences("sandbox_auth", Context.MODE_PRIVATE)

    fun isLoggedIn(): Boolean = prefs.getBoolean(KEY_LOGGED_IN, false)

    fun setLoggedIn(value: Boolean) {
        prefs.edit().putBoolean(KEY_LOGGED_IN, value).apply()
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    private companion object {
        const val KEY_LOGGED_IN = "logged_in"
    }
}
