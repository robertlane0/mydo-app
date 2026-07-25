package com.mydo.app.domain.usecase

import com.mydo.app.core.errors.AppResult
import com.mydo.app.core.time.TimeProvider
import com.mydo.app.domain.model.TaskSummary
import com.mydo.app.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.ZoneId

/**
 * Backs the Today destination (specs00-overview.md: "Today: tasks due today or overdue";
 * specs03-home-screen.md). [TaskRepository.observeTodayTasks] already selects both buckets
 * in one query (`dueAtUtcMillis < endOfDayUtcMillis`); this use case just resolves "end of
 * today" from the device clock/timezone so the screen doesn't have to.
 */
class ObserveTodayTasksUseCase(
    private val taskRepository: TaskRepository,
    private val timeProvider: TimeProvider,
    private val zoneId: ZoneId = ZoneId.systemDefault(),
) {
    operator fun invoke(): Flow<AppResult<List<TaskSummary>>> {
        val startOfToday = Instant.ofEpochMilli(timeProvider.nowUtcMillis()).atZone(zoneId).toLocalDate()
        val endOfToday = startOfToday.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
        return taskRepository.observeTodayTasks(endOfToday)
    }
}
