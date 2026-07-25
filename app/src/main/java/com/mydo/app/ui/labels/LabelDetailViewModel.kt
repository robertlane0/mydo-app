package com.mydo.app.ui.labels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mydo.app.core.errors.AppResult
import com.mydo.app.domain.model.TaskSummary
import com.mydo.app.domain.usecase.CompleteTaskUseCase
import com.mydo.app.domain.usecase.ObserveTasksForLabelUseCase
import com.mydo.app.domain.usecase.UndoCompleteTaskUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

sealed interface LabelDetailUiState {
    data object Loading : LabelDetailUiState
    data class Ready(val tasks: List<TaskSummary>) : LabelDetailUiState
    data class Error(val message: String) : LabelDetailUiState
}

/** One-shot event (an Undo snackbar) the Label Detail screen should surface once and forget. */
sealed interface LabelDetailEvent {
    data class TaskCompleted(val outcome: CompleteTaskUseCase.Outcome) : LabelDetailEvent
}

/**
 * Backs the "tasks for a label" view (specs13-labels.md). [ObserveTasksForLabelUseCase] is
 * a one-shot query rather than a reactive Flow, so this screen refreshes explicitly after
 * any action that could change its membership (most notably completing a task, since
 * completed tasks drop out of the list).
 */
class LabelDetailViewModel(
    private val labelId: UUID,
    private val observeTasksForLabelUseCase: ObserveTasksForLabelUseCase,
    private val completeTaskUseCase: CompleteTaskUseCase,
    private val undoCompleteTaskUseCase: UndoCompleteTaskUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow<LabelDetailUiState>(LabelDetailUiState.Loading)
    val uiState: StateFlow<LabelDetailUiState> = _uiState.asStateFlow()

    val events = MutableSharedFlow<LabelDetailEvent>(extraBufferCapacity = 1)

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            when (val result = observeTasksForLabelUseCase(labelId)) {
                is AppResult.Success -> _uiState.value = LabelDetailUiState.Ready(result.value)
                is AppResult.Failure -> _uiState.value = LabelDetailUiState.Error(result.error.userMessage)
            }
        }
    }

    fun completeTask(id: UUID) {
        viewModelScope.launch {
            val result = completeTaskUseCase(id)
            if (result is AppResult.Success) {
                events.tryEmit(LabelDetailEvent.TaskCompleted(result.value))
                refresh()
            }
        }
    }

    fun undoComplete(outcome: CompleteTaskUseCase.Outcome) {
        viewModelScope.launch {
            undoCompleteTaskUseCase(outcome)
            refresh()
        }
    }

    class Factory(
        private val labelId: UUID,
        private val observeTasksForLabelUseCase: ObserveTasksForLabelUseCase,
        private val completeTaskUseCase: CompleteTaskUseCase,
        private val undoCompleteTaskUseCase: UndoCompleteTaskUseCase,
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return LabelDetailViewModel(labelId, observeTasksForLabelUseCase, completeTaskUseCase, undoCompleteTaskUseCase) as T
        }
    }
}
