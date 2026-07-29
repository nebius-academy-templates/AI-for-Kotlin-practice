package com.sandbox.qa.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sandbox.qa.data.SandboxContract
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
)

/**
 * Holds the OTP code input, the validation error and the resend cooldown
 * timer. No dependencies (the valid code is a sandbox constant), so it is
 * created with the default no-args ViewModel factory.
 */
class OtpViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(OtpUiState())
    val uiState: StateFlow<OtpUiState> = _uiState.asStateFlow()

    private var cooldownJob: Job? = null

    /**
     * Restores the initial screen state: clears the code and the error and
     * restarts the resend cooldown. The screen calls this every time it
     * enters composition, so returning to the OTP screen (system back from
     * the passkey promo) shows a fresh screen. This matches the original
     * remember-based behavior, where the state died with the composition;
     * the ViewModel outlives it because it is scoped to the backstack entry.
     */
    fun reset() {
        _uiState.update { it.copy(code = "", error = null) }
        startCooldown()
    }

    fun onCodeChange(input: String) {
        _uiState.update { it.copy(code = input.filter { c -> c.isDigit() }.take(4)) }
    }

    /** Checks the code; returns true when the UI should navigate on. */
    fun confirm(): Boolean =
        if (_uiState.value.code == SandboxContract.VALID_OTP) {
            _uiState.update { it.copy(error = null) }
            true
        } else {
            _uiState.update { it.copy(error = "Invalid code") }
            false
        }

    /** Restarts the cooldown; the resend button is enabled only at zero. */
    fun resend() {
        startCooldown()
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
}
