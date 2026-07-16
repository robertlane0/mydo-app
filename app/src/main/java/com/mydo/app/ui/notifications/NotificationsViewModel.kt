package com.mydo.app.ui.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mydo.app.core.errors.AppResult
import com.mydo.app.domain.model.Notification
import com.mydo.app.domain.usecase.ClearNotificationsUseCase
import com.mydo.app.domain.usecase.MarkAllNotificationsReadUseCase
import com.mydo.app.domain.usecase.MarkNotificationReadUseCase
import com.mydo.app.domain.usecase.ObserveNotificationsUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

sealed interface NotificationsUiState {
    data object Loading : NotificationsUiState
    data class Ready(val notifications: List<Notification>) : NotificationsUiState
    data class Error(val message: String) : NotificationsUiState
}

class NotificationsViewModel(
    observeNotificationsUseCase: ObserveNotificationsUseCase,
    private val markNotificationReadUseCase: MarkNotificationReadUseCase,
    private val markAllNotificationsReadUseCase: MarkAllNotificationsReadUseCase,
    private val clearNotificationsUseCase: ClearNotificationsUseCase,
) : ViewModel() {

    val uiState: StateFlow<NotificationsUiState> = observeNotificationsUseCase().map {
        when (it) {
            is AppResult.Success -> NotificationsUiState.Ready(it.value)
            is AppResult.Failure -> NotificationsUiState.Error(it.error.userMessage)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), NotificationsUiState.Loading)

    fun markRead(id: UUID) {
        viewModelScope.launch { markNotificationReadUseCase(id) }
    }

    fun markAllRead() {
        viewModelScope.launch { markAllNotificationsReadUseCase() }
    }

    fun clearAll() {
        viewModelScope.launch { clearNotificationsUseCase() }
    }

    class Factory(
        private val observeNotificationsUseCase: ObserveNotificationsUseCase,
        private val markNotificationReadUseCase: MarkNotificationReadUseCase,
        private val markAllNotificationsReadUseCase: MarkAllNotificationsReadUseCase,
        private val clearNotificationsUseCase: ClearNotificationsUseCase,
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return NotificationsViewModel(observeNotificationsUseCase, markNotificationReadUseCase, markAllNotificationsReadUseCase, clearNotificationsUseCase) as T
        }
    }
}
