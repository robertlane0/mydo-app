package com.mydo.app.ui.upcoming

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mydo.app.core.errors.AppResult
import com.mydo.app.core.time.TimeProvider
import com.mydo.app.domain.usecase.ObserveUpcomingUseCase
import com.mydo.app.domain.usecase.RescheduleTaskUseCase
import com.mydo.app.domain.model.TaskSummary
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

private const val INITIAL_WINDOW_DAYS = 30L
private const val WINDOW_EXTENSION_DAYS = 30L

/**
 * Backs the Upcoming timeline (specs07-upcoming.md): overdue tasks up top, then a
 * chronological, lazily-widening window of scheduled tasks grouped by day so distant
 * dates aren't all loaded at once.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class UpcomingViewModel(
    private val observeUpcomingUseCase: ObserveUpcomingUseCase,
    private val rescheduleTaskUseCase: RescheduleTaskUseCase,
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

    private fun groupByDay(tasks: List<TaskSummary>): List<UpcomingDay> {
        val byDate = tasks.groupBy { task ->
            Instant.ofEpochMilli(task.dueAtUtcMillis!!).atZone(zoneId).toLocalDate()
        }
        return byDate.entries.sortedBy { it.key }.map { (date, dayTasks) -> UpcomingDay(date, dayTasks) }
    }

    /** Called when the user scrolls near the bottom of the timeline. */
    fun loadMore() {
        windowDays.value += WINDOW_EXTENSION_DAYS
    }

    fun reschedule(taskId: UUID, newDate: LocalDate) {
        val dueAtUtcMillis = newDate.atTime(12, 0).atZone(zoneId).toInstant().toEpochMilli()
        viewModelScope.launch { rescheduleTaskUseCase(taskId, dueAtUtcMillis) }
    }

    class Factory(
        private val observeUpcomingUseCase: ObserveUpcomingUseCase,
        private val rescheduleTaskUseCase: RescheduleTaskUseCase,
        private val timeProvider: TimeProvider,
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return UpcomingViewModel(observeUpcomingUseCase, rescheduleTaskUseCase, timeProvider) as T
        }
    }
}
