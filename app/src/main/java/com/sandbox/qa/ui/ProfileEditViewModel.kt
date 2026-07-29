package com.sandbox.qa.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.sandbox.qa.di.Profile
import com.sandbox.qa.di.ProfileStore
import com.sandbox.qa.sandboxApplication
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** A pickable city with its region label, shown in the search dialog. */
data class ProfileCity(
    val name: String,
    val region: String,
)

/** The city catalog the picker searches; static because the sandbox is offline. */
val PROFILE_CITIES =
    listOf(
        ProfileCity("Belgrade", "Serbia"),
        ProfileCity("Novi Sad", "Vojvodina, Serbia"),
        ProfileCity("Nis", "Serbia"),
        ProfileCity("Podgorica", "Montenegro"),
        ProfileCity("Budva", "Montenegro"),
        ProfileCity("Limassol", "Cyprus"),
        ProfileCity("Nicosia", "Cyprus"),
        ProfileCity("Tbilisi", "Georgia"),
        ProfileCity("Batumi", "Adjara, Georgia"),
        ProfileCity("Yerevan", "Armenia"),
        ProfileCity("Tashkent", "Uzbekistan"),
        ProfileCity("Almaty", "Kazakhstan"),
        ProfileCity("Astana", "Kazakhstan"),
        ProfileCity("Bishkek", "Kyrgyzstan"),
        ProfileCity("Hurghada", "Red Sea, Egypt"),
    )

data class ProfileEditUiState(
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
    val city: String = "",
    val phoneMasked: String = "",
    /** Set after a successful save; the screen shows the confirmation beat. */
    val saved: Boolean = false,
)

/**
 * Owns the edit form. Loads the current [Profile] from the app-scoped
 * [ProfileStore] once, saves back on demand; the drawer header observes the
 * same store, so a saved name is visible there immediately.
 */
class ProfileEditViewModel(
    private val store: ProfileStore,
) : ViewModel() {
    private val _uiState = MutableStateFlow(fromProfile(store.profile.value))
    val uiState: StateFlow<ProfileEditUiState> = _uiState.asStateFlow()

    fun onFirstNameChange(value: String) = _uiState.update { it.copy(firstName = value, saved = false) }

    fun onLastNameChange(value: String) = _uiState.update { it.copy(lastName = value, saved = false) }

    fun onEmailChange(value: String) = _uiState.update { it.copy(email = value, saved = false) }

    fun onCitySelected(city: String) = _uiState.update { it.copy(city = city, saved = false) }

    /** Case-insensitive substring match over the city catalog. */
    fun citiesMatching(query: String): List<ProfileCity> {
        val q = query.trim()
        if (q.isEmpty()) return PROFILE_CITIES
        return PROFILE_CITIES.filter { it.name.contains(q, ignoreCase = true) }
    }

    fun save() {
        val s = _uiState.value
        store.save(
            store.profile.value.copy(
                firstName = s.firstName,
                lastName = s.lastName,
                email = s.email,
                city = s.city,
            ),
        )
        _uiState.update { it.copy(saved = true) }
    }

    private fun fromProfile(p: Profile) =
        ProfileEditUiState(
            firstName = p.firstName,
            lastName = p.lastName,
            email = p.email,
            city = p.city,
            phoneMasked = p.phoneMasked,
        )

    companion object {
        val Factory: ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    ProfileEditViewModel(sandboxApplication().container.profileStore)
                }
            }
    }
}
