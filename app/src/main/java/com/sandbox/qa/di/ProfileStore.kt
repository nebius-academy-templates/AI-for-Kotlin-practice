package com.sandbox.qa.di

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The user profile shown in the drawer header and edited on the profile
 * screen. Anonymous by default: an empty first name means "no profile yet",
 * and the drawer shows a placeholder until the user creates one.
 *
 * Persistent, like the sign-in flag: a name entered by a returning user
 * survives a process restart (it would be absurd to remember the login and
 * forget the name). The ConditionReceiver reset broadcast clears it back to
 * anonymous together with the login, so autotests start from a known state,
 * and the launch placeholder "Username" / "No rating yet" stays load-bearing.
 * Profile editing has no backend twin (unlike rides and orders it never
 * touches [com.sandbox.qa.data.RideRepository]).
 */
data class Profile(
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
    val city: String = "",
    /** Shown masked and read-only; changing the number is out of the demo's scope. */
    val phoneMasked: String = "99*****99",
    val rating: String = "No rating yet",
) {
    /** True until the user has created a profile (an anonymous rider). */
    val isAnonymous: Boolean get() = firstName.isBlank()
}

/** App-scoped holder backed by SharedPreferences so a saved profile survives restarts. */
class ProfileStore(
    context: Context,
) {
    private val prefs = context.getSharedPreferences("sandbox_profile", Context.MODE_PRIVATE)
    private val _profile = MutableStateFlow(load())
    val profile: StateFlow<Profile> = _profile.asStateFlow()

    fun save(updated: Profile) {
        prefs
            .edit()
            .putString(KEY_FIRST, updated.firstName)
            .putString(KEY_LAST, updated.lastName)
            .putString(KEY_EMAIL, updated.email)
            .putString(KEY_CITY, updated.city)
            .apply()
        _profile.value = updated
    }

    /** Back to anonymous; called by the reset seam alongside AuthStore.clear(). */
    fun clear() {
        prefs.edit().clear().apply()
        _profile.value = Profile()
    }

    private fun load() =
        Profile(
            firstName = prefs.getString(KEY_FIRST, "").orEmpty(),
            lastName = prefs.getString(KEY_LAST, "").orEmpty(),
            email = prefs.getString(KEY_EMAIL, "").orEmpty(),
            city = prefs.getString(KEY_CITY, "").orEmpty(),
        )

    private companion object {
        const val KEY_FIRST = "first_name"
        const val KEY_LAST = "last_name"
        const val KEY_EMAIL = "email"
        const val KEY_CITY = "city"
    }
}
