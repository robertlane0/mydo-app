package com.mydo.app.domain.usecase

import com.mydo.app.core.errors.AppResult
import com.mydo.app.core.time.TimeProvider
import com.mydo.app.domain.model.TaskSummary
import com.mydo.app.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ObserveUpcomingTasksUseCase(
    private val taskRepository: TaskRepository,
    private val timeProvider: TimeProvider,
) {
    operator fun invoke(): Flow<AppResult<List<UpcomingGroup>>> = taskRepository.observeAllScheduledTasks()
        .map { result ->
            when (result) {
                is AppResult.Failure -> result
                is AppResult.Success -> {
                    val grouped = groupTasksByDate(result.value, timeProvider.nowUtcMillis())
                    AppResult.Success(grouped)
                }
            }
        }

    private fun groupTasksByDate(tasks: List<TaskSummary>, nowUtcMillis: Long): List<UpcomingGroup> {
        val groups = mutableMapOf<Long, MutableList<TaskSummary>>()
        var hasOverdue = false
        
        for (task in tasks) {
            val dueMillis = task.dueAtUtcMillis ?: continue
            val dateMillis = startOfDayUtc(dueMillis)
            
            if (dueMillis < nowUtcMillis && !task.completed) {
                hasOverdue = true
            }
            
            val group = groups.getOrPut(dateMillis) { mutableListOf() }
            group.add(task)
        }
        
        val sortedGroups = groups.entries.sortedBy { it.key }.map { (date, tasks) ->
            UpcomingGroup(
                dateMillis = date,
                tasks = tasks,
                isOverdue = date < startOfDayUtc(nowUtcMillis)
            )
        }
        
        // Add overdue group at the beginning if there are overdue tasks
        if (hasOverdue) {
            val overdueTasks = tasks.filter { it.dueAtUtcMillis != null && it.dueAtUtcMillis!! < nowUtcMillis && !it.completed }
            if (overdueTasks.isNotEmpty()) {
                return listOf(UpcomingGroup(
                    dateMillis = Long.MIN_VALUE,
                    tasks = overdueTasks,
                    isOverdue = true
                )) + sortedGroups
            }
        }
        
        return sortedGroups
    }
    
    private fun startOfDayUtc(utcMillis: Long): Long {
        val calendar = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"))
        calendar.timeInMillis = utcMillis
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }
}

data class UpcomingGroup(
    val dateMillis: Long,
    val tasks: List<TaskSummary>,
    val isOverdue: Boolean = false
) {
    val isToday: Boolean
        get() {
            val now = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"))
            val todayStart = now.timeInMillis
            now.set(java.util.Calendar.HOUR_OF_DAY, 0)
            now.set(java.util.Calendar.MINUTE, 0)
            now.set(java.util.Calendar.SECOND, 0)
            now.set(java.util.Calendar.MILLISECOND, 0)
            return dateMillis == now.timeInMillis
        }
    
    val isTomorrow: Boolean
        get() {
            val calendar = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"))
            calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
            calendar.set(java.util.Calendar.MINUTE, 0)
            calendar.set(java.util.Calendar.SECOND, 0)
            calendar.set(java.util.Calendar.MILLISECOND, 0)
            calendar.add(java.util.Calendar.DAY_OF_MONTH, 1)
            return dateMillis == calendar.timeInMillis
        }
}