package com.sandbox.qa.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Beats of the FAKE local passkey creation, deterministic on purpose: the
 * sandbox has no credential APIs (they would need Play services and break
 * offline determinism), so "creation" is a fixed spinner beat followed by a
 * fixed success beat.
 */
private const val CREATING_MS = 900L
private const val CREATED_MS = 800L

data class PasskeyCreateUiState(
    /** false = "Creating your passkey..." beat, true = "Passkey created" beat. */
    val created: Boolean = false,
    /** Set after the success beat: the screen navigates on. */
    val done: Boolean = false,
)

class PasskeyCreateViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(PasskeyCreateUiState())
    val uiState: StateFlow<PasskeyCreateUiState> = _uiState.asStateFlow()

    private var started = false

    /** Idempotent: recomposition must not restart the beats. */
    fun start() {
        if (started) return
        started = true
        viewModelScope.launch {
            delay(CREATING_MS)
            _uiState.update { it.copy(created = true) }
            delay(CREATED_MS)
            _uiState.update { it.copy(done = true) }
        }
    }
}
