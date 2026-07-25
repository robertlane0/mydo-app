package com.mydo.app.ui.today

import com.mydo.app.domain.model.SortMode
import com.mydo.app.domain.model.TaskSummary
import java.util.UUID

sealed interface TodayUiState {
    data object Loading : TodayUiState

    /** [overdue] and [dueToday] are pre-split by [com.mydo.app.ui.today.TodayViewModel]
     *  (specs00-overview.md: "Today: tasks due today or overdue"). */
    data class Ready(
        val overdue: List<TaskSummary>,
        val dueToday: List<TaskSummary>,
        val sortMode: SortMode = SortMode.MANUAL,
        val selectedIds: Set<UUID> = emptySet(),
        val selectionMode: Boolean = false,
    ) : TodayUiState {
        val isEmpty: Boolean get() = overdue.isEmpty() && dueToday.isEmpty()

        /** Overdue tasks in display order for the current [sortMode]. */
        val orderedOverdue: List<TaskSummary> get() = order(overdue)

        /** Today's tasks in display order for the current [sortMode] (manual keeps DB sortOrder). */
        val orderedDueToday: List<TaskSummary> get() = order(dueToday)

        private fun order(tasks: List<TaskSummary>): List<TaskSummary> = when (sortMode) {
            SortMode.MANUAL -> tasks.sortedBy { it.sortOrder }
            SortMode.DUE_DATE -> tasks.sortedWith(compareBy(nullsLast()) { it.dueAtUtcMillis })
            SortMode.PRIORITY -> tasks.sortedBy { it.priority.ordinal }
            SortMode.NAME -> tasks.sortedBy { it.title.lowercase() }
            SortMode.CREATED -> tasks
        }
    }
    data class Error(val message: String) : TodayUiState
}
