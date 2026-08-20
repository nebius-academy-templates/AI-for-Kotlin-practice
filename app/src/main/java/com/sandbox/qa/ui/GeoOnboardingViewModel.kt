package com.sandbox.qa.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.sandbox.qa.data.ApiException
import com.sandbox.qa.data.RideRepository
import com.sandbox.qa.di.LocationStore
import com.sandbox.qa.sandboxApplication
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class GeoOnboardingUiState(
    val processing: Boolean = false,
    val error: String? = null,
    /** One-shot success flag; the UI navigates to the map when it turns true. */
    val granted: Boolean = false,
)

/** Owns the geolocation request and the processing overlay state. */
class GeoOnboardingViewModel(
    private val repository: RideRepository,
    private val locationStore: LocationStore,
) : ViewModel() {
    private val _uiState = MutableStateFlow(GeoOnboardingUiState())
    val uiState: StateFlow<GeoOnboardingUiState> = _uiState.asStateFlow()

    fun requestGeolocation() {
        if (_uiState.value.processing) return
        viewModelScope.launch {
            _uiState.update { it.copy(processing = true, error = null) }
            try {
                locationStore.savePickup(repository.requestGeolocation())
                // Keep processing = true on success: the overlay must stay
                // visible until the granted flag navigates away and this
                // screen is popped, otherwise the geo screen flashes for one
                // frame without the overlay. Matches the original screen,
                // which called onDone() while the overlay was still shown.
                // Only the error path clears processing.
                _uiState.update { it.copy(granted = true) }
            } catch (e: ApiException) {
                _uiState.update { it.copy(processing = false, error = e.message) }
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    val container = sandboxApplication().container
                    GeoOnboardingViewModel(container.rideRepository, container.locationStore)
                }
            }
    }
}
