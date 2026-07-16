package com.mydo.app.ui.labels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mydo.app.core.errors.AppResult
import com.mydo.app.domain.model.TaskSummary
import com.mydo.app.domain.usecase.ObserveTasksForLabelUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

sealed interface LabelDetailUiState {
    data object Loading : LabelDetailUiState
    data class Ready(val tasks: List<TaskSummary>) : LabelDetailUiState
    data class Error(val message: String) : LabelDetailUiState
}

class LabelDetailViewModel(
    private val labelId: UUID,
    private val observeTasksForLabelUseCase: ObserveTasksForLabelUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow<LabelDetailUiState>(LabelDetailUiState.Loading)
    val uiState: StateFlow<LabelDetailUiState> = _uiState.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            when (val result = observeTasksForLabelUseCase(labelId)) {
                is AppResult.Success -> _uiState.value = LabelDetailUiState.Ready(result.value)
                is AppResult.Failure -> _uiState.value = LabelDetailUiState.Error(result.error.userMessage)
            }
        }
    }

    class Factory(
        private val labelId: UUID,
        private val observeTasksForLabelUseCase: ObserveTasksForLabelUseCase,
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return LabelDetailViewModel(labelId, observeTasksForLabelUseCase) as T
        }
    }
}
