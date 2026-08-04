package com.sandbox.qa.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.sandbox.qa.data.ApiException
import com.sandbox.qa.data.AuthRepository
import com.sandbox.qa.sandboxApplication
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val RESEND_COOLDOWN_SEC = 30

data class OtpUiState(
    val code: String = "",
    val error: String? = null,
    val secondsLeft: Int = RESEND_COOLDOWN_SEC,
    val loading: Boolean = false,
    val signedIn: Boolean = false,
)

/**
 * Holds the OTP code input, verifies it with fake-api and owns the resend
 * cooldown timer.
 */
class OtpViewModel(
    private val phone: String,
    private val authRepository: AuthRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(OtpUiState())
    val uiState: StateFlow<OtpUiState> = _uiState.asStateFlow()

    private var cooldownJob: Job? = null

    init {
        startCooldown()
    }

    fun onCodeChange(input: String) {
        _uiState.update { it.copy(code = input.filter { c -> c.isDigit() }.take(4)) }
    }

    /** Sends the code to fake-api and navigates only after it issues a token. */
    fun confirm() {
        val state = _uiState.value
        if (state.loading) return
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null) }
            try {
                authRepository.verifyOtp(phone, state.code)
                _uiState.update { it.copy(loading = false, signedIn = true) }
            } catch (e: ApiException) {
                _uiState.update { it.copy(loading = false, error = e.message) }
            }
        }
    }

    fun onSignedInHandled() {
        _uiState.value = OtpUiState()
        startCooldown()
    }

    /** Restarts the cooldown; the resend button is enabled only at zero. */
    fun resend() {
        if (_uiState.value.loading) return
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null) }
            try {
                authRepository.requestOtp(phone)
                _uiState.update { it.copy(loading = false) }
                startCooldown()
            } catch (e: ApiException) {
                _uiState.update { it.copy(loading = false, error = e.message) }
            }
        }
    }

    private fun startCooldown() {
        cooldownJob?.cancel()
        cooldownJob =
            viewModelScope.launch {
                _uiState.update { it.copy(secondsLeft = RESEND_COOLDOWN_SEC) }
                while (_uiState.value.secondsLeft > 0) {
                    delay(1_000)
                    _uiState.update { it.copy(secondsLeft = it.secondsLeft - 1) }
                }
            }
    }

    companion object {
        fun factory(phone: String): ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    OtpViewModel(phone, sandboxApplication().container.authRepository)
                }
            }
    }
}
