package com.sandbox.qa.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.sandbox.qa.data.ApiException
import com.sandbox.qa.data.Order
import com.sandbox.qa.data.RideRepository
import com.sandbox.qa.sandboxApplication
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class OrderHistoryUiState(
    val isLoading: Boolean = true,
    val orders: List<Order> = emptyList(),
    val error: String? = null,
)

/** Loads order history when the ViewModel is created. */
class OrderHistoryViewModel(
    private val repository: RideRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(OrderHistoryUiState())
    val uiState: StateFlow<OrderHistoryUiState> = _uiState.asStateFlow()

    init {
        loadOrders()
    }

    private fun loadOrders() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    error = null,
                )
            }

            try {
                val orders = repository.getOrders()

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        orders = orders,
                        error = null,
                    )
                }
            } catch (e: ApiException) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        orders = emptyList(),
                        error = e.message ?: "Failed to load order history",
                    )
                }
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    OrderHistoryViewModel(
                        sandboxApplication().container.rideRepository,
                    )
                }
            }
    }
}
