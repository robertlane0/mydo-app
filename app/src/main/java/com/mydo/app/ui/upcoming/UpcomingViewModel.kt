package com.mydo.app.ui.upcoming

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mydo.app.core.errors.AppResult
import com.mydo.app.core.time.TimeProvider
import com.mydo.app.domain.model.TaskSummary
import com.mydo.app.domain.usecase.CompleteTaskUseCase
import com.mydo.app.domain.usecase.ObserveUpcomingUseCase
import com.mydo.app.domain.usecase.RescheduleTaskUseCase
import com.mydo.app.domain.usecase.UndoCompleteTaskUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.UUID

private const val INITIAL_WINDOW_DAYS = 30L
private const val WINDOW_EXTENSION_DAYS = 30L

/** One-shot event the Upcoming screen should surface once and forget (an Undo snackbar). */
sealed interface UpcomingEvent {
    data class TaskCompleted(val outcome: CompleteTaskUseCase.Outcome) : UpcomingEvent
}

/**
 * Backs the Upcoming timeline (specs07-upcoming.md): overdue tasks up top, then a
 * chronological, lazily-widening window of scheduled tasks grouped by day so distant
 * dates aren't all loaded at once.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class UpcomingViewModel(
    private val observeUpcomingUseCase: ObserveUpcomingUseCase,
    private val rescheduleTaskUseCase: RescheduleTaskUseCase,
    private val completeTaskUseCase: CompleteTaskUseCase,
    private val undoCompleteTaskUseCase: UndoCompleteTaskUseCase,
    private val timeProvider: TimeProvider,
    private val zoneId: ZoneId = ZoneId.systemDefault(),
) : ViewModel() {

    private val windowDays = MutableStateFlow(INITIAL_WINDOW_DAYS)

    val uiState: StateFlow<UpcomingUiState> = windowDays.flatMapLatest { days ->
        val today = LocalDate.now(zoneId)
        val start = today.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val end = today.plusDays(days).atStartOfDay(zoneId).toInstant().toEpochMilli()
        observeUpcomingUseCase(start, end)
    }.map { result ->
        when (result) {
            is AppResult.Failure -> UpcomingUiState.Error(result.error.userMessage)
            is AppResult.Success -> UpcomingUiState.Ready(
                overdue = result.value.overdue,
                days = groupByDay(result.value.scheduled),
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UpcomingUiState.Loading)

    val events = MutableSharedFlow<UpcomingEvent>(extraBufferCapacity = 1)

    private fun groupByDay(tasks: List<TaskSummary>): List<UpcomingDay> {
        // Every task here came from observeOverdue/observeScheduledWindow, both of which
        // filter to dueAtUtcMillis IS NOT NULL, so this is always non-null in practice;
        // fall back to "today" rather than crashing if that invariant is ever violated.
        val byDate = tasks.groupBy { task ->
            task.dueAtUtcMillis?.let { Instant.ofEpochMilli(it).atZone(zoneId).toLocalDate() } ?: LocalDate.now(zoneId)
        }
        return byDate.entries.sortedBy { it.key }.map { (date, dayTasks) -> UpcomingDay(date, dayTasks) }
    }

    /** Called when the user scrolls near the bottom of the timeline. */
    fun loadMore() {
        windowDays.value += WINDOW_EXTENSION_DAYS
    }

    /**
     * Reschedules [taskId] to [newDate], preserving its existing time-of-day
     * (specs18-drag-reorder.md, "Rescheduling in Upcoming": "Time preserved"). Tasks with
     * no prior due time default to noon.
     */
    fun reschedule(taskId: UUID, newDate: LocalDate, existingDueAtUtcMillis: Long? = null) {
        val timeOfDay = existingDueAtUtcMillis
            ?.let { Instant.ofEpochMilli(it).atZone(zoneId).toLocalTime() }
            ?: LocalTime.NOON
        val dueAtUtcMillis = newDate.atTime(timeOfDay).atZone(zoneId).toInstant().toEpochMilli()
        viewModelScope.launch { rescheduleTaskUseCase(taskId, dueAtUtcMillis) }
    }

    fun completeTask(id: UUID) {
        viewModelScope.launch {
            val result = completeTaskUseCase(id)
            if (result is AppResult.Success) events.tryEmit(UpcomingEvent.TaskCompleted(result.value))
        }
    }

    fun undoComplete(outcome: CompleteTaskUseCase.Outcome) {
        viewModelScope.launch { undoCompleteTaskUseCase(outcome) }
    }

    class Factory(
        private val observeUpcomingUseCase: ObserveUpcomingUseCase,
        private val rescheduleTaskUseCase: RescheduleTaskUseCase,
        private val completeTaskUseCase: CompleteTaskUseCase,
        private val undoCompleteTaskUseCase: UndoCompleteTaskUseCase,
        private val timeProvider: TimeProvider,
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return UpcomingViewModel(
                observeUpcomingUseCase, rescheduleTaskUseCase, completeTaskUseCase, undoCompleteTaskUseCase, timeProvider,
            ) as T
        }
    }
}
