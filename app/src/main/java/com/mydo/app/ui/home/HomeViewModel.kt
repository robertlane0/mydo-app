package com.mydo.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mydo.app.core.errors.AppResult
import com.mydo.app.domain.usecase.ObserveInboxTasksUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class HomeViewModel(
    observeInboxTasks: ObserveInboxTasksUseCase,
) : ViewModel() {
    val uiState: StateFlow<HomeUiState> = observeInboxTasks()
        .map { result ->
            when (result) {
                is AppResult.Failure -> HomeUiState.Error(result.error.userMessage)
                is AppResult.Success -> HomeUiState.Ready(result.value)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = HomeUiState.Loading,
        )

    class Factory(
        private val observeInboxTasks: ObserveInboxTasksUseCase,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
                return HomeViewModel(observeInboxTasks) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
