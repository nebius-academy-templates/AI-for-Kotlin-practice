package com.sandbox.qa.di

import android.content.Context
import android.provider.Settings
import com.sandbox.qa.BuildConfig
import com.sandbox.qa.data.AuthRepository
import com.sandbox.qa.data.HttpRideRepository
import com.sandbox.qa.data.RideRepository

/**
 * Manual dependency injection container, created once by
 * [com.sandbox.qa.SandboxApplication] and shared by every ViewModel factory.
 *
 * Deliberately hand-rolled (no Hilt, no service locator): at this scale a
 * single class holding app-scoped singletons is the whole DI story, matching
 * Google's small official samples.
 */
class AppContainer(
    context: Context,
) {
    /** Persistent fake-api bearer token; its presence is the sign-in state. */
    val authStore: AuthStore = AuthStore(context)

    private val httpRideRepository =
        HttpRideRepository(
            baseUrl = BuildConfig.API_BASE_URL,
            sandboxSessionId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID),
            tokenProvider = authStore::currentToken,
            onTokenChanged = authStore::setToken,
        )

    /** App-scoped client of the deterministic Ktor backend running on the host. */
    val rideRepository: RideRepository = httpRideRepository

    /** The same HTTP client, narrowed to the authentication surface for auth ViewModels. */
    val authRepository: AuthRepository = httpRideRepository

    /** Enables the explicit `authenticated=true` Appium entrance only. */
    fun enableTestAuthentication() {
        httpRideRepository.enableTestAuthentication()
    }

    /** Device-local profile demo data; see [ProfileStore] for why it has no backend twin. */
    val profileStore: ProfileStore = ProfileStore(context)

    /** Last pickup resolved during location onboarding. */
    val locationStore: LocationStore = LocationStore(context)

    /** Persistent in-app inbox populated by the ride lifecycle. */
    val notificationStore: NotificationStore = NotificationStore(context)
}
