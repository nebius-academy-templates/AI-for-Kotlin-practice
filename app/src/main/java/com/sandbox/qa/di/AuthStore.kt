package com.sandbox.qa.di

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Persistent fake-api bearer token. Its presence is the single source of
 * truth for sign-in state and survives process restarts on purpose.
 *
 * The test seam: the ConditionReceiver reset broadcast
 * (`--ez reset true`) clears this token together with the sandbox states, and
 * the test base class sends that broadcast after every test - so autotests
 * always start from a known entrance while manual sessions keep their login.
 */
class AuthStore(
    context: Context,
) {
    private val prefs = context.getSharedPreferences("sandbox_auth", Context.MODE_PRIVATE)
    private val _token = MutableStateFlow(prefs.getString(KEY_TOKEN, null))
    val token: StateFlow<String?> = _token.asStateFlow()

    fun currentToken(): String? = _token.value

    fun isLoggedIn(): Boolean = currentToken() != null

    fun setToken(value: String?) {
        _token.value = value
        prefs
            .edit()
            .apply {
                if (value == null) remove(KEY_TOKEN) else putString(KEY_TOKEN, value)
            }.apply()
    }

    fun clear() {
        setToken(null)
    }

    private companion object {
        const val KEY_TOKEN = "token"
    }
}
