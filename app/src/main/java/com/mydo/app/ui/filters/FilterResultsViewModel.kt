package com.mydo.app.ui.filters

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mydo.app.core.errors.AppResult
import com.mydo.app.domain.model.Filter
import com.mydo.app.domain.model.TaskSummary
import com.mydo.app.domain.repository.FilterRepository
import com.mydo.app.domain.usecase.CompleteTaskUseCase
import com.mydo.app.domain.usecase.RunFilterUseCase
import com.mydo.app.domain.usecase.UndoCompleteTaskUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

sealed interface FilterResultsUiState {
    data object Loading : FilterResultsUiState
    data class Ready(val filter: Filter, val tasks: List<TaskSummary>) : FilterResultsUiState
    data class Error(val message: String) : FilterResultsUiState
}

/** One-shot event (an Undo snackbar) the Filter Results screen should surface once and forget. */
sealed interface FilterResultsEvent {
    data class TaskCompleted(val outcome: CompleteTaskUseCase.Outcome) : FilterResultsEvent
}

/**
 * Loads a saved filter by id, then runs its query (specs14-filters.md, "Filter Results").
 * [RunFilterUseCase] is a one-shot query, so this screen refreshes explicitly after
 * completing a task (so it drops out of the results, matching the spec's "same task row
 * layout... same bulk operations" parity with other list screens).
 */
class FilterResultsViewModel(
    private val filterId: UUID,
    private val filterRepository: FilterRepository,
    private val runFilterUseCase: RunFilterUseCase,
    private val completeTaskUseCase: CompleteTaskUseCase,
    private val undoCompleteTaskUseCase: UndoCompleteTaskUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow<FilterResultsUiState>(FilterResultsUiState.Loading)
    val uiState: StateFlow<FilterResultsUiState> = _uiState.asStateFlow()

    val events = MutableSharedFlow<FilterResultsEvent>(extraBufferCapacity = 1)

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            val filterResult = filterRepository.getById(filterId)
            val filter = when (filterResult) {
                is AppResult.Failure -> { _uiState.value = FilterResultsUiState.Error(filterResult.error.userMessage); return@launch }
                is AppResult.Success -> filterResult.value ?: run {
                    _uiState.value = FilterResultsUiState.Error("This filter no longer exists")
                    return@launch
                }
            }
            when (val result = runFilterUseCase(filter.query)) {
                is AppResult.Success -> _uiState.value = FilterResultsUiState.Ready(filter, result.value)
                is AppResult.Failure -> _uiState.value = FilterResultsUiState.Error(result.error.userMessage)
            }
        }
    }

    fun completeTask(id: UUID) {
        viewModelScope.launch {
            val result = completeTaskUseCase(id)
            if (result is AppResult.Success) {
                events.tryEmit(FilterResultsEvent.TaskCompleted(result.value))
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
        private val filterId: UUID,
        private val filterRepository: FilterRepository,
        private val runFilterUseCase: RunFilterUseCase,
        private val completeTaskUseCase: CompleteTaskUseCase,
        private val undoCompleteTaskUseCase: UndoCompleteTaskUseCase,
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return FilterResultsViewModel(filterId, filterRepository, runFilterUseCase, completeTaskUseCase, undoCompleteTaskUseCase) as T
        }
    }
}
