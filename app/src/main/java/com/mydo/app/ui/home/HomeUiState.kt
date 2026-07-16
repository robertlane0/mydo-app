package com.mydo.app.ui.home

import com.mydo.app.domain.model.SortMode
import com.mydo.app.domain.model.TaskSummary

sealed interface HomeUiState {
    data object Loading : HomeUiState
    data class Ready(
        val tasks: List<TaskSummary>,
        val sortMode: SortMode = SortMode.MANUAL,
        val selectedIds: Set<java.util.UUID> = emptySet(),
        val selectionMode: Boolean = false,
    ) : HomeUiState {
        /** Tasks in display order for the current [sortMode] (manual keeps DB sortOrder). */
        val orderedTasks: List<TaskSummary>
            get() = when (sortMode) {
                SortMode.MANUAL -> tasks.sortedBy { it.sortOrder }
                SortMode.DUE_DATE -> tasks.sortedWith(compareBy(nullsLast()) { it.dueAtUtcMillis })
                SortMode.PRIORITY -> tasks.sortedBy { it.priority.ordinal }
                SortMode.NAME -> tasks.sortedBy { it.title.lowercase() }
                SortMode.CREATED -> tasks
            }
    }
    data class Error(val message: String) : HomeUiState
}
