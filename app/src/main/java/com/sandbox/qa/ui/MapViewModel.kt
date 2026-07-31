package com.sandbox.qa.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.sandbox.qa.data.ActiveRide
import com.sandbox.qa.data.ActiveRideExistsException
import com.sandbox.qa.data.ApiException
import com.sandbox.qa.data.DriverNotFoundException
import com.sandbox.qa.data.Order
import com.sandbox.qa.data.RideOption
import com.sandbox.qa.data.RideRepository
import com.sandbox.qa.di.NotificationStore
import com.sandbox.qa.sandboxApplication
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private enum class ActiveRideRestoreState {
    LOADING,
    NONE,
    PRESENT,
    UNAVAILABLE,
}

data class MapUiState(
    val pickup: String,
    val destination: String = "",
    /** null until the first successful search (nothing to render yet). */
    val rides: List<RideOption>? = null,
    /**
     * The tariff the user tapped in the offers list. Selection HIGHLIGHTS
     * the row; the other tariffs stay visible and the choice can be
     * switched. A new search resets it. (The first shipped version closed
     * the list on selection - the classic overreaction bug; the options
     * must not change after a choice.)
     */
    val selectedRideId: Int? = null,
    val loading: Boolean = false,
    val error: String? = null,
    val searchingForDriver: Boolean = false,
    val rideActionLoading: Boolean = false,
    val rideActionError: String? = null,
    val activeRide: ActiveRide? = null,
    val completedOrder: Order? = null,
    val statusMessage: String? = null,
)

/**
 * Owns the pickup and destination inputs plus the ride search lifecycle.
 *
 * State-driven UI is deliberately NOT here: the region_unavailable banner and
 * the car_unavailable tariff filter read [com.sandbox.qa.condition.ConditionConfig]
 * snapshot state directly in MapScreen, so a mid-session adb broadcast
 * re-renders instantly without a StateFlow bridge.
 */
class MapViewModel(
    private val repository: RideRepository,
    private val notificationStore: NotificationStore,
    initialPickup: String,
) : ViewModel() {
    private val _uiState = MutableStateFlow(MapUiState(pickup = initialPickup))
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()
    private var routeSearchJob: Job? = null
    private var restoreGeneration = 0L
    private var activeRideRestoreState = ActiveRideRestoreState.LOADING

    init {
        restoreActiveRide()
    }

    fun onPickupChange(value: String) {
        routeSearchJob?.cancel()
        _uiState.update {
            it.copy(
                pickup = value,
                rides = null,
                selectedRideId = null,
                loading = false,
                error = null,
                rideActionError = null,
                statusMessage = null,
            )
        }
        scheduleRouteSearchIfComplete()
    }

    fun onDestinationChange(value: String) {
        routeSearchJob?.cancel()
        _uiState.update {
            it.copy(
                destination = value,
                rides = null,
                selectedRideId = null,
                loading = false,
                error = null,
                rideActionError = null,
                statusMessage = null,
            )
        }
        scheduleRouteSearchIfComplete()
    }

    private fun scheduleRouteSearchIfComplete() {
        val state = _uiState.value
        if (state.pickup.isBlank() || state.destination.isBlank()) return
        routeSearchJob =
            viewModelScope.launch {
                delay(ROUTE_DEBOUNCE_MS)
                loadRides(keepCurrentRides = false)
            }
    }

    fun onRideSelected(id: Int) {
        _uiState.update {
            it.copy(
                selectedRideId = id,
                rideActionError = null,
                statusMessage = null,
            )
        }
    }

    fun refreshRides() {
        if (_uiState.value.loading) return
        routeSearchJob?.cancel()
        routeSearchJob =
            viewModelScope.launch {
                loadRides(keepCurrentRides = true)
            }
    }

    private suspend fun loadRides(keepCurrentRides: Boolean) {
        val route = _uiState.value
        if (route.pickup.isBlank() || route.destination.isBlank() || route.loading) return
        _uiState.update {
            it.copy(
                loading = true,
                error = null,
                rides = if (keepCurrentRides) it.rides else null,
                selectedRideId = null,
                statusMessage = null,
            )
        }
        try {
            val rides = repository.getRideOptions(route.pickup, route.destination)
            _uiState.update { it.copy(loading = false, rides = rides) }
        } catch (e: ApiException) {
            _uiState.update { it.copy(loading = false, error = e.message) }
        }
    }

    fun orderSelectedRide() {
        startDriverSearch(clearCompletedOrder = false)
    }

    fun bookAnotherRide() {
        startDriverSearch(clearCompletedOrder = true)
    }

    fun returnHomeAfterRide() {
        invalidatePendingRestore()
        activeRideRestoreState = ActiveRideRestoreState.NONE
        _uiState.update {
            it.copy(
                selectedRideId = null,
                activeRide = null,
                completedOrder = null,
                rideActionError = null,
                statusMessage = null,
            )
        }
    }

    private fun startDriverSearch(clearCompletedOrder: Boolean) {
        val state = _uiState.value
        val rideId = state.selectedRideId ?: return
        if (state.rideActionLoading || state.searchingForDriver) return
        invalidatePendingRestore()
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    searchingForDriver = true,
                    rideActionError = null,
                    statusMessage = null,
                    completedOrder = if (clearCompletedOrder) null else it.completedOrder,
                )
            }
            // Keep the search as a real, observable product state instead of a
            // sub-frame transition on a fast local backend.
            delay(MIN_DRIVER_SEARCH_MS)
            try {
                val ride = repository.createRide(state.pickup, state.destination, rideId)
                notificationStore.recordDriverFound(ride)
                activeRideRestoreState = ActiveRideRestoreState.PRESENT
                _uiState.update {
                    it.copy(
                        searchingForDriver = false,
                        activeRide = ride,
                        statusMessage = null,
                    )
                }
            } catch (_: ActiveRideExistsException) {
                _uiState.update { it.copy(searchingForDriver = false, rideActionError = null) }
                restoreActiveRide()
            } catch (e: DriverNotFoundException) {
                activeRideRestoreState = ActiveRideRestoreState.NONE
                _uiState.update {
                    it.copy(
                        searchingForDriver = false,
                        activeRide = null,
                        selectedRideId = null,
                        statusMessage = e.message,
                    )
                }
            } catch (e: ApiException) {
                _uiState.update {
                    it.copy(
                        searchingForDriver = false,
                        rideActionError = e.message,
                    )
                }
            }
        }
    }

    fun completeRide() {
        val ride = _uiState.value.activeRide ?: return
        if (_uiState.value.rideActionLoading) return
        invalidatePendingRestore()
        viewModelScope.launch {
            _uiState.update { it.copy(rideActionLoading = true, rideActionError = null) }
            try {
                val order = repository.completeRide(ride.id)
                notificationStore.recordRideCompleted(order)
                activeRideRestoreState = ActiveRideRestoreState.NONE
                _uiState.update {
                    it.copy(
                        rideActionLoading = false,
                        activeRide = null,
                        completedOrder = order,
                    )
                }
            } catch (e: ApiException) {
                _uiState.update { it.copy(rideActionLoading = false, rideActionError = e.message) }
            }
        }
    }

    fun cancelRide() {
        val ride = _uiState.value.activeRide ?: return
        if (_uiState.value.rideActionLoading) return
        invalidatePendingRestore()
        viewModelScope.launch {
            _uiState.update { it.copy(rideActionLoading = true, rideActionError = null) }
            try {
                val cancelledRide = repository.cancelRide(ride.id)
                notificationStore.recordRideCancelled(cancelledRide)
                activeRideRestoreState = ActiveRideRestoreState.NONE
                _uiState.update {
                    it.copy(
                        rideActionLoading = false,
                        activeRide = null,
                        selectedRideId = null,
                        statusMessage = "Ride cancelled",
                    )
                }
            } catch (e: ApiException) {
                _uiState.update { it.copy(rideActionLoading = false, rideActionError = e.message) }
            }
        }
    }

    private fun restoreActiveRide() {
        val requestGeneration = ++restoreGeneration
        activeRideRestoreState = ActiveRideRestoreState.LOADING
        viewModelScope.launch {
            val result = runCatching { repository.getActiveRide() }
            if (
                requestGeneration != restoreGeneration ||
                activeRideRestoreState != ActiveRideRestoreState.LOADING
            ) {
                return@launch
            }
            result.fold(
                onSuccess = { ride ->
                    activeRideRestoreState =
                        if (ride == null) {
                            ActiveRideRestoreState.NONE
                        } else {
                            ActiveRideRestoreState.PRESENT
                        }
                    if (ride != null) {
                        _uiState.update { it.copy(activeRide = ride) }
                    }
                },
                onFailure = {
                    // Restoration is best-effort and must not block or add an
                    // error banner to the ride form. A later create conflict
                    // reconciles with the server through this same endpoint.
                    activeRideRestoreState = ActiveRideRestoreState.UNAVAILABLE
                },
            )
        }
    }

    private fun invalidatePendingRestore() {
        if (activeRideRestoreState != ActiveRideRestoreState.LOADING) {
            return
        }
        restoreGeneration++
        activeRideRestoreState = ActiveRideRestoreState.UNAVAILABLE
    }

    companion object {
        private const val ROUTE_DEBOUNCE_MS = 350L
        private const val MIN_DRIVER_SEARCH_MS = 1_200L

        val Factory: ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    val container = sandboxApplication().container
                    MapViewModel(
                        repository = container.rideRepository,
                        notificationStore = container.notificationStore,
                        initialPickup = container.locationStore.currentPickup(),
                    )
                }
            }
    }
}
