package com.mydo.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mydo.app.core.errors.AppResult
import com.mydo.app.domain.model.Priority
import com.mydo.app.domain.model.SortMode
import com.mydo.app.domain.usecase.BulkActionOutcome
import com.mydo.app.domain.usecase.BulkAddLabelsOutcome
import com.mydo.app.domain.usecase.BulkAddLabelsUseCase
import com.mydo.app.domain.usecase.BulkCompleteTasksUseCase
import com.mydo.app.domain.usecase.BulkDeleteTasksUseCase
import com.mydo.app.domain.usecase.BulkMoveTasksUseCase
import com.mydo.app.domain.usecase.BulkSetDueDateUseCase
import com.mydo.app.domain.usecase.BulkSetPriorityUseCase
import com.mydo.app.domain.usecase.CompleteTaskUseCase
import com.mydo.app.domain.usecase.ObserveInboxTasksUseCase
import com.mydo.app.domain.usecase.ReorderTasksUseCase
import com.mydo.app.domain.usecase.UndoBulkAddLabelsUseCase
import com.mydo.app.domain.usecase.UndoBulkTaskOperationUseCase
import com.mydo.app.domain.usecase.UndoCompleteTaskUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

/** One-shot events (undo snackbars) the screen should surface once and forget. */
sealed interface HomeEvent {
    data class TaskCompleted(val outcome: CompleteTaskUseCase.Outcome) : HomeEvent
    data class BulkActionDone(val label: String, val outcome: BulkActionOutcome) : HomeEvent
    data class BulkLabelsAdded(val outcome: BulkAddLabelsOutcome) : HomeEvent
}

/**
 * Backs the Inbox list (specs04 MVP + specs17/specs18 step-4 additions: completion with
 * recurrence generation, multi-select bulk actions, sort mode, and manual drag reorder).
 */
class HomeViewModel(
    observeInboxTasks: ObserveInboxTasksUseCase,
    private val completeTaskUseCase: CompleteTaskUseCase,
    private val undoCompleteTaskUseCase: UndoCompleteTaskUseCase,
    private val reorderTasksUseCase: ReorderTasksUseCase,
    private val bulkSetPriorityUseCase: BulkSetPriorityUseCase,
    private val bulkSetDueDateUseCase: BulkSetDueDateUseCase,
    private val bulkMoveTasksUseCase: BulkMoveTasksUseCase,
    private val bulkCompleteTasksUseCase: BulkCompleteTasksUseCase,
    private val bulkDeleteTasksUseCase: BulkDeleteTasksUseCase,
    private val undoBulkTaskOperationUseCase: UndoBulkTaskOperationUseCase,
    private val bulkAddLabelsUseCase: BulkAddLabelsUseCase,
    private val undoBulkAddLabelsUseCase: UndoBulkAddLabelsUseCase,
) : ViewModel() {

    private val sortMode = MutableStateFlow(SortMode.MANUAL)
    private val selectedIds = MutableStateFlow<Set<UUID>>(emptySet())
    private val selectionMode = MutableStateFlow(false)

    val uiState: StateFlow<HomeUiState> = combine(
        observeInboxTasks(),
        sortMode,
        selectedIds,
        selectionMode,
    ) { result, sort, selected, selecting ->
        when (result) {
            is AppResult.Success -> HomeUiState.Ready(result.value, sort, selected, selecting)
            is AppResult.Failure -> HomeUiState.Error(result.error.userMessage)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState.Loading)

    val events = MutableSharedFlow<HomeEvent>()

    fun setSortMode(mode: SortMode) { sortMode.value = mode }

    fun toggleSelectionMode() {
        selectionMode.value = !selectionMode.value
        if (!selectionMode.value) selectedIds.value = emptySet()
    }

    fun toggleSelected(id: UUID) {
        selectedIds.value = if (selectedIds.value.contains(id)) selectedIds.value - id else selectedIds.value + id
    }

    fun clearSelection() {
        selectedIds.value = emptySet()
        selectionMode.value = false
    }

    fun completeTask(id: UUID) {
        viewModelScope.launch {
            val result = completeTaskUseCase(id)
            if (result is AppResult.Success) events.emit(HomeEvent.TaskCompleted(result.value))
        }
    }

    fun undoComplete(outcome: CompleteTaskUseCase.Outcome) {
        viewModelScope.launch { undoCompleteTaskUseCase(outcome) }
    }

    /** Persists a drag-reorder; [newOrder] is the full visible list in its new order. */
    fun reorder(newOrder: List<UUID>) {
        viewModelScope.launch { reorderTasksUseCase(newOrder) }
    }

    fun bulkComplete() = runBulk("Marked complete") { bulkCompleteTasksUseCase(it) }
    fun bulkSetPriority(priority: Priority) = runBulk("Priority updated") { bulkSetPriorityUseCase(it, priority) }
    fun bulkSetDueDate(dueAtUtcMillis: Long?) = runBulk("Due date updated") { bulkSetDueDateUseCase(it, dueAtUtcMillis) }
    fun bulkMove(projectId: UUID?, sectionId: UUID?) = runBulk("Moved") { bulkMoveTasksUseCase(it, projectId, sectionId) }

    fun bulkAddLabels(labelIds: List<UUID>) {
        val ids = selectedIds.value.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            val result = bulkAddLabelsUseCase(ids, labelIds)
            if (result is AppResult.Success) events.emit(HomeEvent.BulkLabelsAdded(result.value))
            clearSelection()
        }
    }

    fun undoBulkLabels(outcome: BulkAddLabelsOutcome) {
        viewModelScope.launch { undoBulkAddLabelsUseCase(outcome) }
    }

    fun bulkDelete() {
        val ids = selectedIds.value.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            bulkDeleteTasksUseCase(ids)
            clearSelection()
        }
    }

    fun undoBulk(outcome: BulkActionOutcome) {
        viewModelScope.launch { undoBulkTaskOperationUseCase(outcome) }
    }

    private fun runBulk(label: String, action: suspend (List<UUID>) -> AppResult<BulkActionOutcome>) {
        val ids = selectedIds.value.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            val result = action(ids)
            if (result is AppResult.Success) events.emit(HomeEvent.BulkActionDone(label, result.value))
            clearSelection()
        }
    }

    class Factory(
        private val observeInboxTasks: ObserveInboxTasksUseCase,
        private val completeTaskUseCase: CompleteTaskUseCase,
        private val undoCompleteTaskUseCase: UndoCompleteTaskUseCase,
        private val reorderTasksUseCase: ReorderTasksUseCase,
        private val bulkSetPriorityUseCase: BulkSetPriorityUseCase,
        private val bulkSetDueDateUseCase: BulkSetDueDateUseCase,
        private val bulkMoveTasksUseCase: BulkMoveTasksUseCase,
        private val bulkCompleteTasksUseCase: BulkCompleteTasksUseCase,
        private val bulkDeleteTasksUseCase: BulkDeleteTasksUseCase,
        private val undoBulkTaskOperationUseCase: UndoBulkTaskOperationUseCase,
        private val bulkAddLabelsUseCase: BulkAddLabelsUseCase,
        private val undoBulkAddLabelsUseCase: UndoBulkAddLabelsUseCase,
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return HomeViewModel(
                observeInboxTasks, completeTaskUseCase, undoCompleteTaskUseCase, reorderTasksUseCase,
                bulkSetPriorityUseCase, bulkSetDueDateUseCase, bulkMoveTasksUseCase, bulkCompleteTasksUseCase,
                bulkDeleteTasksUseCase, undoBulkTaskOperationUseCase, bulkAddLabelsUseCase, undoBulkAddLabelsUseCase,
            ) as T
        }
    }
}
