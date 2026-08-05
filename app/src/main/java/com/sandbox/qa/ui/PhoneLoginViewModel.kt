package com.sandbox.qa.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.sandbox.qa.data.ApiException
import com.sandbox.qa.data.AuthRepository
import com.sandbox.qa.data.SandboxContract
import com.sandbox.qa.sandboxApplication
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val MIN_LOCAL_PHONE_DIGITS = 8

/** The number the login screen offers for quick manual runs. No special backend behavior: the OTP stays 1234. */
const val TEST_PHONE_NUMBER = SandboxContract.TEST_PHONE_NUMBER

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
    val loading: Boolean = false,
    val otpRequestedFor: String? = null,
)

/**
 * Holds the phone input and requests the deterministic OTP from fake-api.
 */
class PhoneLoginViewModel(
    private val authRepository: AuthRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(PhoneLoginUiState())
    val uiState: StateFlow<PhoneLoginUiState> = _uiState.asStateFlow()

    fun onPhoneChange(input: String) {
        _uiState.update {
            it.copy(
                phone = input.filter(Char::isDigit),
                error = null,
            )
        }
    }

    fun onRegionSelected(region: PhoneRegion) {
        _uiState.update { it.copy(region = region, error = null) }
    }

    /** Fills the input with [TEST_PHONE_NUMBER]; see the load-bearing values in AGENTS.md. */
    fun useTestNumber() {
        _uiState.update { it.copy(phone = TEST_PHONE_NUMBER, error = null) }
    }

    /** Validates the phone and requests an OTP from fake-api. */
    fun submit() {
        val state = _uiState.value
        if (state.loading) return
        if (state.phone.length < MIN_LOCAL_PHONE_DIGITS) {
            _uiState.update { it.copy(error = "Enter a valid phone number") }
            return
        }

        val fullPhone = state.region.code + state.phone
        _uiState.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            try {
                authRepository.requestOtp(fullPhone)
                _uiState.update { it.copy(loading = false, otpRequestedFor = fullPhone) }
            } catch (e: ApiException) {
                _uiState.update { it.copy(loading = false, error = e.message) }
            }
        }
    }

    fun onOtpRequestHandled() {
        _uiState.value = PhoneLoginUiState()
    }

    companion object {
        val Factory: ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    PhoneLoginViewModel(sandboxApplication().container.authRepository)
                }
            }
    }
}
