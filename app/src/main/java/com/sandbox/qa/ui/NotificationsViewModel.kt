package com.sandbox.qa.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.sandbox.qa.di.NotificationStore
import com.sandbox.qa.di.RideNotification
import com.sandbox.qa.sandboxApplication
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class NotificationsUiState(
    val items: List<RideNotification> = emptyList(),
)

class NotificationsViewModel(
    private val notificationStore: NotificationStore,
) : ViewModel() {
    val uiState: StateFlow<NotificationsUiState> =
        notificationStore.notifications
            .map(::NotificationsUiState)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Eagerly,
                initialValue = NotificationsUiState(notificationStore.notifications.value),
            )

    fun markRead(id: Long) {
        notificationStore.markRead(id)
    }

    fun markAllRead() {
        notificationStore.markAllRead()
    }

    companion object {
        val Factory: ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    NotificationsViewModel(sandboxApplication().container.notificationStore)
                }
            }
    }
}
