package com.sandbox.qa.ui

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

private const val MIN_PHONE_DIGITS = 8

/** The number the login screen offers for quick manual runs. No special backend behavior: the OTP stays 1234. */
const val TEST_PHONE_NUMBER = "999999999"

/** A pickable dialing region. Product stub: enough entries for the picker to be real. */
data class PhoneRegion(
    val flag: String,
    val code: String,
    val name: String,
)

val PHONE_REGIONS =
    listOf(
        PhoneRegion("🇷🇸", "+381", "Serbia"),
        PhoneRegion("🇲🇪", "+382", "Montenegro"),
        PhoneRegion("🇬🇪", "+995", "Georgia"),
    )

data class PhoneLoginUiState(
    val phone: String = "",
    val region: PhoneRegion = PHONE_REGIONS.first(),
    val error: String? = null,
)

/**
 * Holds the phone input and its validation error. No dependencies, so it is
 * created with the default no-args ViewModel factory.
 */
class PhoneLoginViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(PhoneLoginUiState())
    val uiState: StateFlow<PhoneLoginUiState> = _uiState.asStateFlow()

    /**
     * Back to defaults (empty input, default region). The screen calls this
     * every time it enters composition, so navigating back from the OTP
     * screen shows an empty form, matching the original remember-based
     * behavior.
     */
    fun reset() {
        _uiState.value = PhoneLoginUiState()
    }

    fun onPhoneChange(input: String) {
        _uiState.update { it.copy(phone = input.filter { c -> c.isDigit() }) }
    }

    fun onRegionSelected(region: PhoneRegion) {
        _uiState.update { it.copy(region = region) }
    }

    /** Fills the input with [TEST_PHONE_NUMBER]; see the load-bearing values in AGENTS.md. */
    fun useTestNumber() {
        _uiState.update { it.copy(phone = TEST_PHONE_NUMBER, error = null) }
    }

    /** Validates the phone; returns true when the UI should navigate on. */
    fun submit(): Boolean =
        if (_uiState.value.phone.length >= MIN_PHONE_DIGITS) {
            _uiState.update { it.copy(error = null) }
            true
        } else {
            _uiState.update { it.copy(error = "Enter a valid phone number") }
            false
        }
}
