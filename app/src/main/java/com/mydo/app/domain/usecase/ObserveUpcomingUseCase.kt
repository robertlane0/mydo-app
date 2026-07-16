package com.mydo.app.domain.usecase

import com.mydo.app.core.errors.AppResult
import com.mydo.app.core.time.TimeProvider
import com.mydo.app.domain.model.TaskSummary
import com.mydo.app.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

data class UpcomingData(val overdue: List<TaskSummary>, val scheduled: List<TaskSummary>)

/**
 * Feeds the Upcoming timeline (specs07-upcoming.md): overdue tasks (always shown in
 * full) plus every scheduled task within a widening window so the list can lazily load
 * further dates as the user scrolls, rather than materializing the whole future at once.
 */
class ObserveUpcomingUseCase(
    private val taskRepository: TaskRepository,
    private val timeProvider: TimeProvider,
) {
    operator fun invoke(windowStartUtcMillis: Long, windowEndUtcMillis: Long): Flow<AppResult<UpcomingData>> {
        val now = timeProvider.nowUtcMillis()
        return combine(
            taskRepository.observeOverdue(now),
            taskRepository.observeScheduledWindow(windowStartUtcMillis, windowEndUtcMillis),
        ) { overdueResult, scheduledResult ->
            if (overdueResult is AppResult.Failure) return@combine overdueResult
            if (scheduledResult is AppResult.Failure) return@combine scheduledResult
            val overdue = (overdueResult as AppResult.Success).value
            val scheduled = (scheduledResult as AppResult.Success).value
            AppResult.Success(UpcomingData(overdue, scheduled))
        }
    }
}
